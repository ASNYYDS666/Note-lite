package com.note.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.ChatService;
import com.note.service.ai.facade.ChatToken;
import com.note.service.common.util.JwtUtils;
import com.note.service.dto.ChatRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SSE 流式对话控制器集成测试。
 * Mock ChatService（避免依赖外部 AI API），使用真实 JWT 认证。
 * 前置条件：Docker Compose (MySQL + Redis) 已启动。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ChatController SSE 流式集成测试")
class ChatControllerSseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private ChatService chatService;

    private static final String CHAT_URL = "/api/v1/chat";
    private String validToken;

    @BeforeEach
    void generateToken() {
        validToken = "Bearer " + jwtUtils.generateToken(1L, "test_user");
    }

    // ========================================
    // 正常流式对话
    // ========================================

    @Nested
    @DisplayName("正常流式对话")
    class NormalStreaming {

        @BeforeEach
        void setUp() {
            // 模拟 ChatService.ask 返回 3 个 token + DONE
            Flux<ChatToken> mockFlux = Flux.just(
                    ChatToken.answer("你好"),
                    ChatToken.answer("，"),
                    ChatToken.answer("世界"),
                    ChatToken.DONE
            );
            when(chatService.ask(anyLong(), anyString(), anyString(),
                    any(), anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(mockFlux);
        }

        @Test
        @DisplayName("正常流式返回多个 token 并正确结束")
        void shouldStreamTokensAndComplete() throws Exception {
            ChatRequest request = new ChatRequest();
            request.setQuestion("你好");
            request.setScopeType("ALL");
            request.setStyle("concise");

            MvcResult mvcResult = mockMvc.perform(post(CHAT_URL)
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult asyncResult = mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = asyncResult.getResponse().getContentAsString();
            assertThat(content).isNotNull();
            assertThat(content).contains("\"token\"");
            assertThat(content).contains("\"done\":true");
        }
    }

    // ========================================
    // 异常流处理
    // ========================================

    @Nested
    @DisplayName("异常流处理")
    class ErrorHandling {

        @Test
        @DisplayName("ChatService 返回 Flux.error → SSE 返回 error 事件")
        void shouldReturnErrorEventOnServiceError() throws Exception {
            when(chatService.ask(anyLong(), anyString(), anyString(),
                    any(), anyString(), any(), any(), any(), any(), any()))
                    .thenReturn(Flux.error(new RuntimeException("AI service timeout")));

            ChatRequest request = new ChatRequest();
            request.setQuestion("触发错误的提问");
            request.setScopeType("ALL");
            request.setStyle("concise");

            MvcResult mvcResult = mockMvc.perform(post(CHAT_URL)
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult asyncResult = mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = asyncResult.getResponse().getContentAsString();
            assertThat(content).contains("\"error\"");
            assertThat(content).contains("service timeout");
            assertThat(content).contains("\"done\":true");
        }
    }

    // ========================================
    // 身份认证
    // ========================================

    @Nested
    @DisplayName("身份认证")
    class Authentication {

        @Test
        @DisplayName("未带 Token → 返回 403（Spring Security 拦截）")
        void shouldReturn403WithoutToken() throws Exception {
            ChatRequest request = new ChatRequest();
            request.setQuestion("测试");
            request.setScopeType("ALL");
            request.setStyle("concise");

            mockMvc.perform(post(CHAT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========================================
    // 参数校验
    // ========================================

    @Nested
    @DisplayName("参数校验")
    class Validation {

        @Test
        @DisplayName("question 为空 → 返回 200，body 中 code=40001（全局异常处理）")
        void shouldValidateEmptyQuestion() throws Exception {
            ChatRequest request = new ChatRequest();
            request.setQuestion("");
            request.setScopeType("ALL");
            request.setStyle("concise");

            // GlobalExceptionHandler 将 MethodArgumentNotValidException 转为 200 + error code
            MvcResult result = mockMvc.perform(post(CHAT_URL)
                            .header("Authorization", validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).contains("40001");  // PARAM_VALIDATION_FAILED
        }
    }
}

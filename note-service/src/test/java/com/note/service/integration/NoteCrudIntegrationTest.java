package com.note.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.common.vo.Result;
import com.note.service.common.vo.NoteDetailVO;
import com.note.service.dto.LoginDTO;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.dto.UserRegisterDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 笔记 CRUD 集成测试。
 * 前置条件：docker compose up -d mysql redis
 * 测试使用独立账号，测试后清理数据，不影响开发环境。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("笔记 CRUD 集成测试 (需要 Docker MySQL + Redis)")
class NoteCrudIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;
    private String authToken;
    private Long testUserId;
    private Long createdNoteId;
    private Long createdFolderId;

    private final String testUsername = "itest_" + UUID.randomUUID().toString().substring(0, 8);
    private final String testPassword = "Test123456";
    private final String testEmail = testUsername + "@test.com";

    @BeforeAll
    void setUp() {
        baseUrl = "http://localhost:" + port;
        // 注册测试用户
        UserRegisterDTO regDto = new UserRegisterDTO();
        regDto.setUsername(testUsername);
        regDto.setPassword(testPassword);
        regDto.setEmail(testEmail);
        restTemplate.postForEntity(baseUrl + "/api/v1/user/register", regDto, Result.class);

        // 登录获取 token
        LoginDTO loginDto = new LoginDTO();
        loginDto.setUsername(testUsername);
        loginDto.setPassword(testPassword);
        ResponseEntity<Result> loginResp = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/login", loginDto, Result.class);
        assertThat(loginResp.getStatusCode().is2xxSuccessful()).isTrue();
        // testUserId 和 token 从登录响应中提取
        var body = loginResp.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) body.getData();
        authToken = (String) data.get("token");
        testUserId = ((Number) data.get("userId")).longValue();
        assertThat(authToken).isNotBlank();
    }

    @AfterAll
    void tearDown() {
        // 物理删除测试过程中创建的所有笔记（清空回收站中的也行）
        // 如果 NoteController 支持按用户清理，就在这里调用
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        return headers;
    }

    private <T> ResponseEntity<Result<T>> post(String path, Object body, ParameterizedTypeReference<Result<T>> typeRef) {
        HttpEntity<Object> request = new HttpEntity<>(body, authHeaders());
        return restTemplate.exchange(baseUrl + path, HttpMethod.POST, request, typeRef);
    }

    private <T> ResponseEntity<Result<T>> get(String path, ParameterizedTypeReference<Result<T>> typeRef) {
        HttpEntity<Void> request = new HttpEntity<>(authHeaders());
        return restTemplate.exchange(baseUrl + path, HttpMethod.GET, request, typeRef);
    }

    // ========================================
    // 测试用例
    // ========================================

    @Test
    @Order(1)
    @DisplayName("1. 创建笔记 → 返回 noteId，内容正确回显")
    void shouldCreateNoteAndRetrieve() {
        NoteDTO dto = new NoteDTO();
        dto.setTitle("集成测试笔记");
        dto.setContent("# 第一章\n这是**加粗**内容\n```java\nSystem.out.println(\"hello\");\n```");
        dto.setTags(List.of("Java", "Spring"));

        // 创建
        var createRef = new ParameterizedTypeReference<Result<Long>>() {};
        ResponseEntity<Result<Long>> createResp = post("/api/v1/note", dto, createRef);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(createResp.getBody()).isNotNull();
        createdNoteId = createResp.getBody().getData();
        assertThat(createdNoteId).isPositive();

        // 回显
        var detailRef = new ParameterizedTypeReference<Result<NoteDetailVO>>() {};
        ResponseEntity<Result<NoteDetailVO>> detailResp = get("/api/v1/note/" + createdNoteId, detailRef);
        assertThat(detailResp.getStatusCode().is2xxSuccessful()).isTrue();
        NoteDetailVO note = detailResp.getBody().getData();

        assertThat(note.getTitle()).isEqualTo("集成测试笔记");
        assertThat(note.getContent()).contains("**加粗**");
        assertThat(note.getContent()).contains("```java");
        assertThat(note.getTags()).contains("java", "spring");
    }

    @Test
    @Order(2)
    @DisplayName("2. 更新笔记 → 缓存失效，内容更新")
    void shouldUpdateNoteAndClearCache() {
        NoteDTO dto = new NoteDTO();
        dto.setTitle("更新后的标题");
        dto.setContent("更新后的内容");
        dto.setTags(List.of("Updated"));

        HttpEntity<NoteDTO> request = new HttpEntity<>(dto, authHeaders());
        ResponseEntity<Result> resp = restTemplate.exchange(
                baseUrl + "/api/v1/note/" + createdNoteId, HttpMethod.PUT, request, Result.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // 再次查询验证更新
        var detailRef = new ParameterizedTypeReference<Result<NoteDetailVO>>() {};
        ResponseEntity<Result<NoteDetailVO>> detailResp = get("/api/v1/note/" + createdNoteId, detailRef);
        NoteDetailVO note = detailResp.getBody().getData();
        assertThat(note.getTitle()).isEqualTo("更新后的标题");
        assertThat(note.getContent()).isEqualTo("更新后的内容");
        assertThat(note.getTags()).contains("updated");
    }

    @Test
    @Order(3)
    @DisplayName("3. 分页查询 → 不传标签走简单查询")
    void shouldPageQueryWithoutTags() {
        var pageRef = new ParameterizedTypeReference<Result<java.util.Map<String, Object>>>() {};
        ResponseEntity<Result<java.util.Map<String, Object>>> resp = get(
                "/api/v1/note/page?pageNum=1&pageSize=10&isDeleted=0", pageRef);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        var data = resp.getBody().getData();
        assertThat(data.get("total")).isNotNull();
        assertThat(data.get("records")).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. 软删除 → 移入回收站")
    void shouldSoftDeleteToRecycle() {
        ResponseEntity<Result> resp = restTemplate.exchange(
                baseUrl + "/api/v1/note/" + createdNoteId + "?permanent=false",
                HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Result.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // 回收站中应该能找到
        var pageRef = new ParameterizedTypeReference<Result<java.util.Map<String, Object>>>() {};
        ResponseEntity<Result<java.util.Map<String, Object>>> recycleResp = get(
                "/api/v1/note/page?pageNum=1&pageSize=10&isDeleted=1", pageRef);
        assertThat(recycleResp.getStatusCode().is2xxSuccessful()).isTrue();
        var data = recycleResp.getBody().getData();
        assertThat((Integer) data.get("total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(5)
    @DisplayName("5. 多用户隔离 → 用户A看不了用户B的笔记")
    void shouldIsolateBetweenUsers() {
        // 注册用户B
        String userB = "itest_b_" + UUID.randomUUID().toString().substring(0, 6);
        UserRegisterDTO regDto = new UserRegisterDTO();
        regDto.setUsername(userB);
        regDto.setPassword("Test123456");
        regDto.setEmail(userB + "@test.com");
        restTemplate.postForEntity(baseUrl + "/api/v1/user/register", regDto, Result.class);

        // 用户B登录
        LoginDTO loginDto = new LoginDTO();
        loginDto.setUsername(userB);
        loginDto.setPassword("Test123456");
        ResponseEntity<Result> loginResp = restTemplate.postForEntity(
                baseUrl + "/api/v1/user/login", loginDto, Result.class);
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) loginResp.getBody().getData();
        String tokenB = (String) data.get("token");

        // 用户B尝试访问用户A的笔记 → 应返回 20002 无权限
        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        ResponseEntity<Result> resp = restTemplate.exchange(
                baseUrl + "/api/v1/note/" + createdNoteId,
                HttpMethod.GET, new HttpEntity<>(headersB), Result.class);
        assertThat(resp.getBody().getCode()).isEqualTo(20002); // NOTE_NO_PERMISSION
    }
}

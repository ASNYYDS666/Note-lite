package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.ai.facade.AIFacadeFactory;
import com.note.service.ai.facade.EmbeddingFacade;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.util.CryptoUtils;
import com.note.service.entity.AiProviderEntity;
import com.note.service.entity.UserApiProfileEntity;
import com.note.service.mapper.UserApiProfileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class UserApiProfileService {

    private final UserApiProfileMapper mapper;
    private final ObjectMapper objectMapper;
    private final CryptoUtils cryptoUtils;
    private final RestTemplate restTemplate;
    private final AiProviderService aiProviderService;
    private final AIFacadeFactory facadeFactory;

    public UserApiProfileService(UserApiProfileMapper mapper,
                                  ObjectMapper objectMapper,
                                  CryptoUtils cryptoUtils,
                                  AiProviderService aiProviderService,
                                  AIFacadeFactory facadeFactory) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.cryptoUtils = cryptoUtils;
        this.restTemplate = new RestTemplate();
        this.aiProviderService = aiProviderService;
        this.facadeFactory = facadeFactory;
    }

    // ==================== CRUD ====================

    public List<UserApiProfileEntity> listByUser(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserApiProfileEntity>()
                .eq(UserApiProfileEntity::getUserId, userId)
                .orderByDesc(UserApiProfileEntity::getUpdatedAt));
    }

    public UserApiProfileEntity getById(Long id, Long userId) {
        UserApiProfileEntity p = mapper.selectById(id);
        if (p == null || !p.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Profile 不存在");
        }
        return p;
    }

    /**
     * 保存或更新 Profile。draft.id 非 null 则更新，否则新建。
     * 返回持久化后的 Entity。
     */
    public UserApiProfileEntity saveOrUpdate(Long userId,
                                              Long id,
                                              String profileName,
                                              String providerKey,
                                              String apiKey,
                                              String baseUrl,
                                              List<String> enabledModels) {
        if (providerKey == null || providerKey.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Provider 不能为空");
        }

        if (id != null) {
            // Update existing
            UserApiProfileEntity exist = getById(id, userId);
            if (profileName != null && !profileName.isEmpty()) exist.setProfileName(profileName);
            if (providerKey != null) exist.setProviderKey(providerKey);
            if (apiKey != null && !apiKey.isEmpty()) exist.setApiKey(cryptoUtils.encrypt(apiKey));
            if (baseUrl != null) exist.setBaseUrl(baseUrl);
            if (enabledModels != null) {
                try { exist.setEnabledModels(objectMapper.writeValueAsString(enabledModels)); }
                catch (Exception e) { throw new RuntimeException("序列化模型列表失败", e); }
            }
            mapper.updateById(exist);
            log.info("Profile 已更新: id={}, userId={}", id, userId);
            return exist;
        } else {
            // Create new — apiKey may be empty (user fills it later in editor)
            UserApiProfileEntity entity = new UserApiProfileEntity();
            entity.setUserId(userId);
            entity.setProfileName(profileName != null ? profileName : providerKey);
            entity.setProviderKey(providerKey);
            if (apiKey != null && !apiKey.isEmpty()) entity.setApiKey(cryptoUtils.encrypt(apiKey));
            entity.setBaseUrl(baseUrl);
            if (enabledModels != null) {
                try { entity.setEnabledModels(objectMapper.writeValueAsString(enabledModels)); }
                catch (Exception e) { throw new RuntimeException("序列化模型列表失败", e); }
            }
            entity.setIsDefault(0);
            mapper.insert(entity);
            log.info("Profile 已创建: id={}, userId={}, name={}", entity.getId(), userId, entity.getProfileName());
            return entity;
        }
    }

    public void delete(Long id, Long userId) {
        UserApiProfileEntity p = getById(id, userId);
        mapper.deleteById(p.getId());
        log.info("Profile 已删除: id={}, userId={}", id, userId);
    }

    // ==================== 远程模型刷新 ====================

    /**
     * 调用 Provider 的 /v1/models 端点，返回模型列表。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> refreshRemoteModels(String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "Base URL 不能为空");
        }
        String url = baseUrl.replaceAll("/+$", "") + "/models";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);
        } catch (Exception e) {
            log.error("远程模型刷新失败: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.AI_CHAT_FAILED,
                    "无法连接 " + baseUrl + "，请检查 URL 和 API Key");
        }

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("data")) {
            throw new BusinessException(ErrorCode.AI_CHAT_FAILED,
                    "厂商返回格式异常，无法解析模型列表");
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : data) {
            String id = (String) item.get("id");
            if (id != null && !id.isEmpty()) {
                result.add(Map.of("id", id));
            }
        }
        log.info("远程模型刷新成功: baseUrl={}, count={}", baseUrl, result.size());
        return result;
    }

    // ==================== 解密 Key 供内部使用 ====================

    public String decryptApiKey(UserApiProfileEntity profile) {
        return cryptoUtils.decrypt(profile.getApiKey());
    }

    /**
     * 测试 Embedding 连接。
     * 从 ai_model 表自动识别厂商的默认 embedding 模型，调用 /embeddings 端点验证。
     */
    public Map<String, Object> testEmbedding(String providerKey, String apiKey, String baseUrl) {
        if (providerKey == null || providerKey.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "请先选择服务商");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            // 本地 provider 不需要 API Key
            if ("local-embedding".equals(providerKey)) {
                apiKey = "local"; // 占位，Infinity 忽略 Authorization 头
            } else {
                throw new BusinessException(ErrorCode.PARAM_VALIDATION_FAILED, "请先填写 API Key");
            }
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            AiProviderEntity provider = aiProviderService.getProvider(providerKey);
            baseUrl = provider.getBaseUrl();
        }

        // 从 ai_model 表获取默认 embedding 模型
        String embedModel = aiProviderService.getDefaultEmbedModel(providerKey);
        if (embedModel == null) {
            throw new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND,
                    "该服务商未预置 Embedding 模型，请先联系管理员在 ai_model 表中配置");
        }

        AiProviderEntity provider = aiProviderService.getProvider(providerKey);
        EmbedAIConfig config = EmbedAIConfig.builder()
                .provider(providerKey)
                .apiKey(apiKey)
                .model(embedModel)
                .baseUrl(baseUrl)
                .pluginType(provider.getPluginType())
                .build();

        EmbeddingFacade facade = facadeFactory.getEmbedding(config.getPluginType());
        long t0 = System.currentTimeMillis();
        List<Float> vector = facade.embed("test", config);
        long latency = System.currentTimeMillis() - t0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("embedModel", embedModel);
        result.put("dimension", vector.size());
        result.put("latencyMs", latency);
        result.put("baseUrl", baseUrl);
        log.info("Embedding 测试成功: provider={}, model={}, dim={}, latency={}ms",
                providerKey, embedModel, vector.size(), latency);
        return result;
    }
}

package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.util.CryptoUtils;
import com.note.service.entity.AiModelEntity;
import com.note.service.entity.AiProviderEntity;
import com.note.service.mapper.AiModelMapper;
import com.note.service.mapper.AiProviderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiProviderService {

    private final AiProviderMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final CryptoUtils cryptoUtils;

    public AiProviderService(AiProviderMapper providerMapper,
                             AiModelMapper modelMapper,
                             CryptoUtils cryptoUtils) {
        this.providerMapper = providerMapper;
        this.modelMapper = modelMapper;
        this.cryptoUtils = cryptoUtils;
    }

    // ==================== 查询 ====================

    public List<AiProviderEntity> listEnabledProviders() {
        return providerMapper.selectList(
                new LambdaQueryWrapper<AiProviderEntity>()
                        .eq(AiProviderEntity::getIsEnabled, 1)
                        .orderByAsc(AiProviderEntity::getId));
    }

    public AiProviderEntity getProvider(String key) {
        AiProviderEntity provider = providerMapper.selectOne(
                new LambdaQueryWrapper<AiProviderEntity>()
                        .eq(AiProviderEntity::getProviderKey, key));
        if (provider == null) {
            throw new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND, "厂商不存在: " + key);
        }
        return provider;
    }

    public List<AiModelEntity> listModels(String providerKey, String modelType) {
        var query = new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getProviderKey, providerKey);
        if (modelType != null) {
            query.eq(AiModelEntity::getModelType, modelType);
        }
        return modelMapper.selectList(query.orderByDesc(AiModelEntity::getIsDefault));
    }

    /**
     * 批量查询所有已启用提供商的模型，按 providerKey 分组返回，替代 N+1 的逐条查询。
     */
    public Map<String, List<AiModelEntity>> listModelsGroupedByProvider() {
        List<AiProviderEntity> providers = listEnabledProviders();
        List<String> keys = providers.stream()
                .map(AiProviderEntity::getProviderKey)
                .toList();
        if (keys.isEmpty()) return Map.of();

        List<AiModelEntity> all = modelMapper.selectList(
                new LambdaQueryWrapper<AiModelEntity>()
                        .in(AiModelEntity::getProviderKey, keys)
                        .orderByDesc(AiModelEntity::getIsDefault));

        return all.stream().collect(Collectors.groupingBy(AiModelEntity::getProviderKey));
    }

    /**
     * 获取厂商的默认 Embedding 模型名，若未配置则返回 null。
     */
    public String getDefaultEmbedModel(String providerKey) {
        AiModelEntity model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModelEntity>()
                        .eq(AiModelEntity::getProviderKey, providerKey)
                        .eq(AiModelEntity::getModelType, "EMBEDDING")
                        .eq(AiModelEntity::getIsDefault, 1));
        return model != null ? model.getModelName() : null;
    }

    // ==================== ChatAIConfig / EmbedAIConfig 构建 ====================

    public ChatAIConfig buildChatConfig(String providerKey, String modelName) {
        return buildChatConfig(providerKey, modelName, null);
    }

    public ChatAIConfig buildChatConfig(String providerKey, String modelName, String customBaseUrl) {
        AiProviderEntity provider = getProvider(providerKey);
        String apiKey = provider.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND,
                    "厂商 " + provider.getName() + " 未配置 API Key");
        }
        String baseUrl = (customBaseUrl != null && !customBaseUrl.isEmpty())
                ? customBaseUrl : provider.getBaseUrl();
        return ChatAIConfig.builder()
                .provider(providerKey)
                .apiKey(cryptoUtils.decrypt(apiKey))
                .model(modelName)
                .baseUrl(baseUrl)
                .pluginType(provider.getPluginType())
                .build();
    }

    public EmbedAIConfig buildEmbedConfig(String providerKey, String modelName) {
        return buildEmbedConfig(providerKey, modelName, null);
    }

    public EmbedAIConfig buildEmbedConfig(String providerKey, String modelName, String customBaseUrl) {
        AiProviderEntity provider = getProvider(providerKey);
        String apiKey = provider.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND,
                    "厂商 " + provider.getName() + " 未配置 API Key");
        }
        String baseUrl = (customBaseUrl != null && !customBaseUrl.isEmpty())
                ? customBaseUrl : provider.getBaseUrl();
        // 本地 provider 的 apiKey 是占位符明文，不需要解密
        String decryptedKey = "local-embedding".equals(providerKey)
                ? apiKey : cryptoUtils.decrypt(apiKey);
        return EmbedAIConfig.builder()
                .provider(providerKey)
                .apiKey(decryptedKey)
                .model(modelName)
                .baseUrl(baseUrl)
                .pluginType(provider.getPluginType())
                .build();
    }

    public String getPluginType(String providerKey) {
        return getProvider(providerKey).getPluginType();
    }

    // ==================== 更新 ====================

    public void updateApiKey(String providerKey, String apiKey) {
        AiProviderEntity provider = getProvider(providerKey);
        provider.setApiKey(cryptoUtils.encrypt(apiKey));
        providerMapper.updateById(provider);
        log.info("API Key 已更新: provider={}", providerKey);
    }
}
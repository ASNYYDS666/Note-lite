package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.entity.AiModelEntity;
import com.note.service.entity.AiProviderEntity;
import com.note.service.mapper.AiModelMapper;
import com.note.service.mapper.AiProviderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class AiProviderService {

    private final AiProviderMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final String aesKey;

    public AiProviderService(AiProviderMapper providerMapper,
                             AiModelMapper modelMapper,
                             @Value("${ai.aes-key}") String aesKey) {
        this.providerMapper = providerMapper;
        this.modelMapper = modelMapper;
        this.aesKey = aesKey;
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
                .apiKey(decrypt(apiKey))
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
        return EmbedAIConfig.builder()
                .provider(providerKey)
                .apiKey(decrypt(apiKey))
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
        provider.setApiKey(encrypt(apiKey));
        providerMapper.updateById(provider);
        log.info("API Key 已更新: provider={}", providerKey);
    }

    // ==================== 加密工具 ====================

    private String encrypt(String plainText) {
        try {
            SecretKeySpec key = new SecretKeySpec(aesKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("API Key 加密失败", e);
        }
    }

    private String decrypt(String encryptedText) {
        try {
            SecretKeySpec key = new SecretKeySpec(aesKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("API Key 解密失败", e);
        }
    }
}
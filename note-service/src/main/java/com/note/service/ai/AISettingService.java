package com.note.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.service.ai.config.ChatAIConfig;
import com.note.service.ai.config.EmbedAIConfig;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.dto.AISettingSaveDTO;
import com.note.service.entity.UserAIConfigEntity;
import com.note.service.mapper.UserAIConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class AISettingService {

    private final UserAIConfigMapper mapper;
    private final String aesKey;

    public AISettingService(UserAIConfigMapper mapper,
                            @Value("${ai.aes-key}") String aesKey) {
        this.mapper = mapper;
        this.aesKey = aesKey;
    }

    // ==================== 查询 ====================

    public UserAIConfigVO getByUserId(Long userId) {
        UserAIConfigEntity config = mapper.selectOne(
                new LambdaQueryWrapper<UserAIConfigEntity>()
                        .eq(UserAIConfigEntity::getUserId, userId));
        if (config == null) {
            return null;
        }
        return toVO(config);
    }

    public ChatAIConfig getDecryptedChatConfig(Long userId) {
        UserAIConfigEntity config = getEntity(userId);
        return ChatAIConfig.builder()
                .provider(config.getChatProvider())
                .apiKey(decrypt(config.getChatApiKey()))
                .model(config.getChatModel())
                .baseUrl(normalizeUrl(config.getChatUrl()))
                .build();
    }

    public EmbedAIConfig getDecryptedEmbedConfig(Long userId) {
        UserAIConfigEntity config = getEntity(userId);
        return EmbedAIConfig.builder()
                .provider(config.getEmbedProvider())
                .apiKey(decrypt(config.getEmbedApiKey()))
                .model(config.getEmbedModel())
                .baseUrl(normalizeUrl(config.getEmbedUrl()))
                .build();
    }

    private UserAIConfigEntity getEntity(Long userId) {
        UserAIConfigEntity config = mapper.selectOne(
                new LambdaQueryWrapper<UserAIConfigEntity>()
                        .eq(UserAIConfigEntity::getUserId, userId));
        if (config == null) {
            throw new BusinessException(ErrorCode.AI_CONFIG_NOT_FOUND);
        }
        if (config.getIsEnabled() != 1) {
            throw new BusinessException(ErrorCode.AI_CONFIG_DISABLED);
        }
        return config;
    }

    // ==================== 保存 ====================

    public void saveOrUpdate(Long userId, AISettingSaveDTO dto) {
        UserAIConfigEntity exist = mapper.selectOne(
                new LambdaQueryWrapper<UserAIConfigEntity>()
                        .eq(UserAIConfigEntity::getUserId, userId));

        if (exist != null) {
            exist.setChatProvider(dto.getChatProvider());
            exist.setChatApiKey(encrypt(dto.getChatApiKey()));
            exist.setChatModel(dto.getChatModel() != null ? dto.getChatModel() : "deepseek-chat");
            exist.setChatUrl(trimToNull(dto.getChatUrl()));
            exist.setEmbedProvider(dto.getEmbedProvider());
            exist.setEmbedApiKey(encrypt(dto.getEmbedApiKey()));
            exist.setEmbedModel(dto.getEmbedModel() != null ? dto.getEmbedModel() : "text-embedding-3-small");
            exist.setEmbedUrl(trimToNull(dto.getEmbedUrl()));
            mapper.updateById(exist);
            log.info("AI 配置已更新: userId={}, chat={}, embed={}",
                    userId, dto.getChatProvider(), dto.getEmbedProvider());
        } else {
            UserAIConfigEntity config = new UserAIConfigEntity();
            config.setUserId(userId);
            config.setChatProvider(dto.getChatProvider());
            config.setChatApiKey(encrypt(dto.getChatApiKey()));
            config.setChatModel(dto.getChatModel() != null ? dto.getChatModel() : "deepseek-chat");
            config.setChatUrl(trimToNull(dto.getChatUrl()));
            config.setEmbedProvider(dto.getEmbedProvider());
            config.setEmbedApiKey(encrypt(dto.getEmbedApiKey()));
            config.setEmbedModel(dto.getEmbedModel() != null ? dto.getEmbedModel() : "text-embedding-3-small");
            config.setEmbedUrl(trimToNull(dto.getEmbedUrl()));
            config.setIsEnabled(1);
            mapper.insert(config);
            log.info("AI 配置已创建: userId={}, chat={}, embed={}",
                    userId, dto.getChatProvider(), dto.getEmbedProvider());
        }
    }

    // ==================== 删除 ====================

    public void delete(Long userId) {
        UserAIConfigEntity config = mapper.selectOne(
                new LambdaQueryWrapper<UserAIConfigEntity>()
                        .eq(UserAIConfigEntity::getUserId, userId));
        if (config != null) {
            mapper.deleteById(config.getId());
            log.info("AI 配置已删除: userId={}", userId);
        }
    }

    // ==================== VO 转换 ====================

    private UserAIConfigVO toVO(UserAIConfigEntity entity) {
        UserAIConfigVO vo = new UserAIConfigVO();
        vo.setChatProvider(entity.getChatProvider());
        vo.setChatModel(entity.getChatModel());
        vo.setChatUrl(entity.getChatUrl());
        vo.setEmbedProvider(entity.getEmbedProvider());
        vo.setEmbedModel(entity.getEmbedModel());
        vo.setEmbedUrl(entity.getEmbedUrl());
        vo.setIsEnabled(entity.getIsEnabled());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
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

    private String trimToNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }

    private String normalizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

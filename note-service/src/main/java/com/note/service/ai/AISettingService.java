package com.note.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.note.service.common.exception.BusinessException;
import com.note.service.common.exception.ErrorCode;
import com.note.service.common.vo.UserAIConfigVO;
import com.note.service.dto.AISettingSaveDTO;
import com.note.service.entity.UserAIConfigEntity;
import com.note.service.mapper.UserAIConfigMapper;
import com.note.service.service.AiProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AISettingService {

    private final UserAIConfigMapper mapper;
    private final AiProviderService aiProviderService;

    public AISettingService(UserAIConfigMapper mapper,
                            AiProviderService aiProviderService) {
        this.mapper = mapper;
        this.aiProviderService = aiProviderService;
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

    /**
     * 获取用户的 Embedding 配置（仅 embedding 相关字段）。
     * 如果用户没有 user_ai_config 行，返回 null。
     */
    public UserAIConfigVO getEmbeddingConfig(Long userId) {
        return getByUserId(userId);
    }

    /**
     * 保存用户的 Embedding 配置。
     * 已有行则更新 embedding 字段，无行则创建（chat 字段走 DB 默认值）。
     */
    @Transactional
    public void saveEmbeddingConfig(Long userId, String embedProvider,
                                     String embedModel, String embedUrl) {
        UserAIConfigEntity exist = mapper.selectOne(
                new LambdaQueryWrapper<UserAIConfigEntity>()
                        .eq(UserAIConfigEntity::getUserId, userId));

        if (exist != null) {
            exist.setEmbedProvider(embedProvider);
            exist.setEmbedModel(embedModel);
            if (embedUrl != null) exist.setEmbedUrl(embedUrl);
            mapper.updateById(exist);
        } else {
            UserAIConfigEntity config = new UserAIConfigEntity();
            config.setUserId(userId);
            config.setEmbedProvider(embedProvider);
            config.setEmbedModel(embedModel);
            config.setEmbedUrl(embedUrl);
            config.setChatProvider("");
            config.setChatModel("");
            config.setChatUrl("");
            config.setIsEnabled(1);
            mapper.insert(config);
        }
        log.info("Embedding 配置已保存: userId={}, provider={}, model={}",
                userId, embedProvider, embedModel);
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

    /**
     * 保存用户 AI 配置。
     * chatApiKey/embedApiKey 写入 ai_provider 表；
     * provider key + model 引用写入 user_ai_config 表。
     */
    @Transactional
    public void saveOrUpdate(Long userId, AISettingSaveDTO dto) {
        // 将 API Key 写入 ai_provider 表
        if (dto.getChatProvider() != null && dto.getChatApiKey() != null) {
            aiProviderService.updateApiKey(dto.getChatProvider(), dto.getChatApiKey());
        }
        if (dto.getEmbedProvider() != null && dto.getEmbedApiKey() != null) {
            aiProviderService.updateApiKey(dto.getEmbedProvider(), dto.getEmbedApiKey());
        }

        UserAIConfigEntity exist = mapper.selectOne(
                new LambdaQueryWrapper<UserAIConfigEntity>()
                        .eq(UserAIConfigEntity::getUserId, userId));

        if (exist != null) {
            exist.setChatProvider(dto.getChatProvider());
            exist.setChatModel(dto.getChatModel() != null ? dto.getChatModel() : "deepseek-chat");
            exist.setChatUrl(dto.getChatUrl());
            if (dto.getEmbedProvider() != null) {
                exist.setEmbedProvider(dto.getEmbedProvider());
            }
            if (dto.getEmbedModel() != null) {
                exist.setEmbedModel(dto.getEmbedModel());
            } else if (dto.getEmbedProvider() != null) {
                exist.setEmbedModel(null);
            }
            if (dto.getEmbedUrl() != null) {
                exist.setEmbedUrl(dto.getEmbedUrl());
            }
            mapper.updateById(exist);
            log.info("AI 配置已更新: userId={}, chat={}, embed={}",
                    userId, dto.getChatProvider(), dto.getEmbedProvider());
        } else {
            UserAIConfigEntity config = new UserAIConfigEntity();
            config.setUserId(userId);
            config.setChatProvider(dto.getChatProvider());
            config.setChatModel(dto.getChatModel() != null ? dto.getChatModel() : "deepseek-chat");
            config.setChatUrl(dto.getChatUrl());
            config.setEmbedProvider(dto.getEmbedProvider());
            config.setEmbedModel(dto.getEmbedModel());
            config.setEmbedUrl(dto.getEmbedUrl());
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
}

package com.note.service.ai.config;

import com.note.service.entity.AiProviderEntity;
import com.note.service.mapper.AiProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 Flyway V7 迁移已执行。
 * 厂商和模型的预置数据由 V7__create_ai_provider.sql 负责插入，
 * 本类仅做启动日志记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderInitializer implements CommandLineRunner {

    private final AiProviderMapper providerMapper;

    @Override
    public void run(String... args) {
        long count = providerMapper.selectCount(null);
        log.info("AI 厂商初始化完成: {} 个厂商已就绪", count);
    }
}

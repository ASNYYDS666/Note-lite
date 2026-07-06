package com.note.service.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ThreadPoolTaskExecutor webAsyncExecutor;

    public WebMvcConfig(@Qualifier("webAsyncExecutor") ThreadPoolTaskExecutor webAsyncExecutor) {
        this.webAsyncExecutor = webAsyncExecutor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(webAsyncExecutor);
        configurer.setDefaultTimeout(120_000);
    }
}

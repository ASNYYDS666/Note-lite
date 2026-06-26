package com.note.service.ai.prompt;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptTemplateEngine {

    private final Mustache.Compiler compiler = Mustache.compiler().escapeHTML(false);
    private final Map<String, Template> cache = new ConcurrentHashMap<>();
    private final boolean hotReload;

    private static final String[] ALL_STYLES = {"concise", "detailed", "code-review"};

    public PromptTemplateEngine(@Value("${spring.profiles.active:dev}") String profile) {
        this.hotReload = "dev".equals(profile);
        log.info("PromptTemplateEngine 初始化: hotReload={}", hotReload);
    }

    /**
     * 启动时预热：prod 环境强制加载所有模板，任一加载失败则终止启动（Fail-Fast）。
     */
    @PostConstruct
    public void preload() {
        if (hotReload) {
            log.info("dev 模式，跳过模板预热");
            return;
        }
        for (String style : ALL_STYLES) {
            try {
                Template t = loadTemplate(style);
                cache.put(style, t);
                log.info("模板加载成功: {}", style);
            } catch (Exception e) {
                throw new IllegalStateException(
                    "模板加载失败，应用终止启动: style=" + style + ", error=" + e.getMessage(), e);
            }
        }
    }

    /**
     * 渲染指定风格的 User Prompt 模板
     * @param style     风格代码 (concise/detailed/code-review)
     * @param variables 模板变量 {hasChunks, chunks, question, truncated}
     * @return 渲染后的提示词文本
     */
    public String render(String style, Map<String, Object> variables) {
        try {
            Template template;
            if (hotReload) {
                template = loadTemplate(style);
            } else {
                template = cache.get(style);
            }
            return template.execute(variables);
        } catch (Exception e) {
            log.error("模板渲染失败: style={}, 降级使用默认 Prompt", style, e);
            return fallbackPrompt(variables);
        }
    }

    /** 清空模板缓存（手动刷新用） */
    public void clearCache() {
        cache.clear();
        log.info("模板缓存已清空");
    }

    private Template loadTemplate(String style) {
        String path = "prompts/" + style + ".md";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return compiler.compile(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("模板文件不存在: " + path, e);
        }
    }

    /**
     * 降级：模板加载/渲染失败时用硬编码默认 Prompt。
     */
    private String fallbackPrompt(Map<String, Object> variables) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据以下笔记片段回答问题。\n\n");

        @SuppressWarnings("unchecked")
        var chunks = (java.util.List<Map<String, Object>>) variables.getOrDefault("chunks", java.util.List.of());
        boolean hasChunks = Boolean.TRUE.equals(variables.get("hasChunks"));

        if (hasChunks && !chunks.isEmpty()) {
            sb.append("相关笔记片段：\n");
            for (var chunk : chunks) {
                sb.append("\n---\n");
                sb.append("[来源 ").append(chunk.get("index")).append(": ");
                sb.append(chunk.getOrDefault("title", "未知笔记")).append("]\n");
                sb.append(chunk.getOrDefault("text", ""));
                sb.append("\n");
            }
        } else {
            sb.append("（无相关笔记片段）\n");
        }

        sb.append("\n问题：").append(variables.getOrDefault("question", ""));
        return sb.toString();
    }
}

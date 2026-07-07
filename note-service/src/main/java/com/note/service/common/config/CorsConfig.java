package com.note.service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（仅开发环境生效）
 * 允许前端 Vite dev server (5173) 访问后端 API (8080)
 *
 * 生产环境不需要：nginx 将前后端统一代理到同一端口，浏览器视为同源请求。
 */
@Profile("dev")
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // ========== 允许的源 ==========
        // 允许前端开发服务器地址（Vite 默认端口 5173）
        config.addAllowedOrigin("http://localhost:5173");
        // 允许 React 或其他前端常用端口（可选，不影响功能）
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:3100");
        // 如果需要部署到线上，可以添加域名
        // config.addAllowedOrigin("https://note.yourdomain.com");

        // ========== 允许的请求头 ==========
        // 允许所有请求头（包括 Authorization、Content-Type 等）
        config.addAllowedHeader("*");

        // ========== 允许的HTTP方法 ==========
        // 允许所有方法（GET, POST, PUT, DELETE, OPTIONS 等）
        config.addAllowedMethod("*");

        // ========== 允许携带凭证 ==========
        // 允许携带 Cookie 和 Authorization 头
        config.setAllowCredentials(true);

        // ========== 暴露响应头 ==========
        // 让前端能读取 Authorization 头（如果 JWT Token 放在 header 里）
        config.addExposedHeader("Authorization");

        // ========== 预检请求缓存 ==========
        // 预检请求结果缓存 1 小时（3600秒），减少 OPTIONS 请求次数
        config.setMaxAge(3600L);

        // 2. 为所有接口注册 CORS 配置
        // 使用 "/**" 匹配所有路径，包括 Swagger 文档等非 /api 路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 3. 返回 CORS 过滤器
        // 这个过滤器会自动注册到 Spring 过滤器链的最前面
        // 确保 OPTIONS 预检请求在到达 Security 之前就被正确处理
        return new CorsFilter(source);
    }
}
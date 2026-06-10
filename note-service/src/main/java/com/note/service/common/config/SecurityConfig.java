//day03
package com.note.service.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;  // 添加这行！
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ========== 1. 启用 CORS ==========
                // 这一行告诉 Spring Security："跨域配置交给 CorsFilter 处理，你不要插手"
                // 必须加，否则 Security 会拦截 OPTIONS 请求
                .cors(Customizer.withDefaults())

                // 禁用 CSRF（前后端分离不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话（不用 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/user/register", "/api/v1/user/login").permitAll()
                        .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/share").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/share/**").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

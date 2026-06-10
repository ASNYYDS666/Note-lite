//day03
package com.note.service.common.config;

import com.note.service.common.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取请求头中的 Authorization
        String header = request.getHeader("Authorization");
        String token = null;

        // 提取 Token（Bearer token）
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        // 验证 Token
        if (token != null && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);

            // 将认证信息存入 SecurityContext（后续可用）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
            authentication.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 验证通过: userId={}, username={}", userId, username);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 登录和注册接口不需要 JWT 验证
        String path = request.getRequestURI();
        return path.equals("/api/v1/user/login")
                || path.equals("/api/v1/user/register")
                || path.startsWith("/doc.html")        // Knife4j 页面
                || path.startsWith("/webjars/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/actuator/");
    }
}

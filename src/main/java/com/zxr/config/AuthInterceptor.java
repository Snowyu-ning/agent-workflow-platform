package com.zxr.config;

import com.zxr.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求
        if ("OPTIONS".equals(request.getMethod())) return true;

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = jwtUtil.parseToken(token);
        if (userId == null) {
            response.setStatus(401);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }

        // 将验证过的 userId 放入上下文
        request.setAttribute("userId", userId);
        return true;
    }
}

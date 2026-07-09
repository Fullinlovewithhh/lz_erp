package com.gcerp.erp.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login")) return true;
        if (path.startsWith("/api/payments/vouchers/files/")) return true;
        if (path.startsWith("/api/master/files/")) return true;
        if (!path.startsWith("/api/")) return true;
        String token = readToken(request);
        CurrentUser user = authService.verify(token);
        if (!authService.hasApiPermission(user, request.getMethod(), path)) {
            throw new IllegalArgumentException("当前账号无权限访问该功能");
        }
        AuthContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String readToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) return "";
        String s = auth.trim();
        if (s.toLowerCase().startsWith("bearer ")) return s.substring(7).trim();
        return s;
    }
}

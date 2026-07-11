package com.gcerp.erp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class LegacyWriteGuardInterceptor implements HandlerInterceptor {
    private static final List<String> LEGACY_PREFIXES = List.of(
            "/api/contracts", "/api/quotes", "/api/quote-details", "/api/payments", "/api/productions"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (isReadMethod(method) || "/api/quote-details/calculate".equals(path) || !isLegacyPath(path)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_GONE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-ERP-Legacy-Write-Disabled", "true");
        response.getWriter().write("{\"code\":-2,\"message\":\"旧订单写入接口已停用，请使用订单管理中的客户订单流程\",\"data\":null}");
        return false;
    }

    private boolean isReadMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    private boolean isLegacyPath(String path) {
        return LEGACY_PREFIXES.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}

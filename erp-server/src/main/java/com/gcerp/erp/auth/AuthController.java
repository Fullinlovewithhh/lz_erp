package com.gcerp.erp.auth;

import com.gcerp.erp.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUser> me() {
        CurrentUser user = AuthContext.get();
        if (user == null) throw new IllegalArgumentException("未登录");
        return ApiResponse.ok(user);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = readToken(request);
        authService.logout(token);
        return ApiResponse.ok(null);
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> listUsers(@RequestParam(required = false) String keyword) {
        requireAdmin();
        return ApiResponse.ok(authService.listUsers(keyword));
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody UserUpsertRequest req) {
        CurrentUser me = requireAdmin();
        return ApiResponse.ok(authService.createUser(req, me.getDisplayName()));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpsertRequest req) {
        CurrentUser me = requireAdmin();
        return ApiResponse.ok(authService.updateUser(id, req, me.getDisplayName()));
    }

    @PutMapping("/users/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserPasswordResetRequest req) {
        CurrentUser me = requireAdmin();
        authService.resetPassword(id, req.getNewPassword(), me.getDisplayName());
        return ApiResponse.ok(null);
    }

    private CurrentUser requireAdmin() {
        CurrentUser me = AuthContext.get();
        if (me == null || !"ADMIN".equalsIgnoreCase(me.getRoleCode())) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        return me;
    }

    private String readToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) return "";
        String s = auth.trim();
        if (s.toLowerCase().startsWith("bearer ")) return s.substring(7).trim();
        return s;
    }
}

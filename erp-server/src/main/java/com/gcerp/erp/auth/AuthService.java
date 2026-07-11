package com.gcerp.erp.auth;

import com.gcerp.erp.audit.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;
    private final Map<String, CurrentUser> tokenStore = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest req) {
        String username = req.getUsername().trim();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, username, password_hash, password_salt, display_name, role_code, enabled
                FROM app_user
                WHERE username = ?
                LIMIT 1
                """, username);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        Map<String, Object> row = rows.getFirst();
        Integer enabled = toInt(row.get("enabled"));
        if (enabled == null || enabled != 1) {
            throw new IllegalArgumentException("账号已禁用");
        }
        String salt = str(row.get("password_salt"));
        String actualHash = str(row.get("password_hash"));
        String inputHash = sha256(req.getPassword() + salt);
        if (!inputHash.equalsIgnoreCase(actualHash)) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        CurrentUser user = buildCurrentUser(row);
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user);
        operationLogService.log("auth", "LOGIN", user.getDisplayName(), "app_user", String.valueOf(user.getId()), null, user, "登录成功");

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUsername(user.getUsername());
        resp.setDisplayName(user.getDisplayName());
        resp.setRoleCode(user.getRoleCode());
        resp.setAllowedModules(user.getAllowedModules());
        return resp;
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        CurrentUser user = tokenStore.remove(token);
        if (user != null) {
            operationLogService.log("auth", "LOGOUT", user.getDisplayName(), "app_user", String.valueOf(user.getId()), null, null, "退出登录");
        }
    }

    public CurrentUser verify(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("未登录或登录已失效");
        }
        CurrentUser user = tokenStore.get(token);
        if (user == null) {
            throw new IllegalArgumentException("登录已过期，请重新登录");
        }
        return user;
    }

    public List<Map<String, Object>> listUsers(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        String like = "%" + kw + "%";
        return jdbcTemplate.queryForList("""
                SELECT id, username, display_name, role_code, enabled, created_at, updated_at
                FROM app_user
                WHERE (? = '' OR username LIKE ? OR display_name LIKE ? OR role_code LIKE ?)
                ORDER BY id DESC
                """, kw, like, like, like);
    }

    public Map<String, Object> createUser(UserUpsertRequest req, String operator) {
        String username = req.getUsername().trim();
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_user WHERE username = ?", Integer.class, username);
        if (cnt != null && cnt > 0) throw new IllegalArgumentException("账号已存在");
        String rawPwd = (req.getPassword() == null || req.getPassword().isBlank()) ? "123456" : req.getPassword().trim();
        if (rawPwd.length() < 6) throw new IllegalArgumentException("密码长度不能小于6位");
        String salt = "LZWN_" + username.toUpperCase();
        String hash = sha256(rawPwd + salt);
        jdbcTemplate.update("""
                INSERT INTO app_user(username, password_hash, password_salt, display_name, role_code, enabled, created_at, updated_at)
                VALUES(?,?,?,?,?,?,NOW(),NOW())
                """, username, hash, salt, req.getDisplayName().trim(), req.getRoleCode().trim(), normalizeEnabled(req.getEnabled()));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
        Map<String, Object> after = getUserById(id);
        operationLogService.log("auth_user", "CREATE", operator, "app_user", String.valueOf(id), null, after, "新增账号");
        return after;
    }

    public Map<String, Object> updateUser(Long id, UserUpsertRequest req, String operator) {
        Map<String, Object> before = getUserById(id);
        jdbcTemplate.update("""
                UPDATE app_user
                SET display_name = ?, role_code = ?, enabled = ?, updated_at = NOW()
                WHERE id = ?
                """, req.getDisplayName().trim(), req.getRoleCode().trim(), normalizeEnabled(req.getEnabled()), id);
        Map<String, Object> after = getUserById(id);
        operationLogService.log("auth_user", "UPDATE", operator, "app_user", String.valueOf(id), before, after, "更新账号信息");
        return after;
    }

    public void resetPassword(Long id, String newPassword, String operator) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("密码长度不能小于6位");
        }
        Map<String, Object> before = getUserById(id);
        String username = String.valueOf(before.get("username"));
        String salt = "LZWN_" + username.toUpperCase();
        String hash = sha256(newPassword.trim() + salt);
        jdbcTemplate.update("UPDATE app_user SET password_hash = ?, password_salt = ?, updated_at = NOW() WHERE id = ?", hash, salt, id);
        operationLogService.log("auth_user", "RESET_PASSWORD", operator, "app_user", String.valueOf(id), null, null, "重置账号密码");
        tokenStore.entrySet().removeIf(e -> e.getValue() != null && id.equals(e.getValue().getId()));
    }

    public boolean hasApiPermission(CurrentUser user, String method, String path) {
        if (user == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) return true;
        String p = path == null ? "" : path;
        if (p.startsWith("/api/auth/me") || p.startsWith("/api/auth/logout")) return true;
        if ("GET".equalsIgnoreCase(method) && p.startsWith("/api/contracts/") && p.endsWith("/logs")) return true;
        return switch (user.getRoleCode()) {
            case "SERVICE" -> startsWithAny(p, "/api/contracts", "/api/customers", "/api/customer-orders", "/api/quote-details/assignments", "/api/quotes", "/api/master/staff");
            case "ENGINEER" -> startsWithAny(p, "/api/quote-details", "/api/quote-rules", "/api/contracts", "/api/customer-orders", "/api/master/products", "/api/master/materials");
            case "FINANCE" -> startsWithAny(p, "/api/payments", "/api/operation-logs", "/api/contracts", "/api/quote-details/quotes");
            case "PRODUCTION" -> startsWithAny(p, "/api/productions", "/api/contracts", "/api/master/products", "/api/master/materials", "/api/master/staff");
            default -> false;
        };
    }

    private CurrentUser buildCurrentUser(Map<String, Object> row) {
        CurrentUser user = new CurrentUser();
        user.setId(toLong(row.get("id")));
        user.setUsername(str(row.get("username")));
        user.setDisplayName(str(row.get("display_name")));
        user.setRoleCode(str(row.get("role_code")));
        user.setAllowedModules(modulesByRole(user.getRoleCode()));
        return user;
    }

    private Map<String, Object> getUserById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                SELECT id, username, display_name, role_code, enabled, created_at, updated_at
                FROM app_user
                WHERE id = ?
                LIMIT 1
                """, id);
        if (list.isEmpty()) throw new IllegalArgumentException("账号不存在");
        return new HashMap<>(list.getFirst());
    }

    private List<String> modulesByRole(String role) {
        LinkedHashMap<String, Boolean> all = new LinkedHashMap<>();
        all.put("基础资料", false);
        all.put("客户管理", false);
        all.put("订单管理", false);
        all.put("报价管理", false);
        all.put("排单管理", false);
        all.put("生产管理", false);
        all.put("采购库存", false);
        all.put("财务数据", false);
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new ArrayList<>(all.keySet());
        }
        if ("DIRECTOR".equalsIgnoreCase(role)) {
            return new ArrayList<>(all.keySet());
        }
        if ("SERVICE".equalsIgnoreCase(role)) {
            all.put("客户管理", true);
            all.put("订单管理", true);
            all.put("报价管理", true);
        } else if ("ENGINEER".equalsIgnoreCase(role)) {
            all.put("报价管理", true);
            all.put("订单管理", true);
        } else if ("FINANCE".equalsIgnoreCase(role)) {
            all.put("订单管理", true);
            all.put("报价管理", true);
            all.put("财务数据", true);
        } else if ("PRODUCTION".equalsIgnoreCase(role)) {
            all.put("订单管理", true);
            all.put("排单管理", true);
            all.put("生产管理", true);
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : all.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) result.add(e.getKey());
        }
        return result;
    }

    private boolean startsWithAny(String path, String... prefixes) {
        for (String p : prefixes) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private String sha256(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8)) + DigestUtils.md5DigestAsHex(("sha2|" + text).getBytes(StandardCharsets.UTF_8));
    }

    private int normalizeEnabled(Integer enabled) {
        return (enabled != null && enabled == 0) ? 0 : 1;
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(v));
    }
}

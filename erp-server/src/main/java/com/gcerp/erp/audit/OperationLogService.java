package com.gcerp.erp.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OperationLogService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OperationLogService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void log(String module, String action, String operator, String targetType, String targetId, Object beforeData, Object afterData, String remark) {
        String op = normalizeOperator(operator);
        jdbcTemplate.update(
                "INSERT INTO operation_log(module, action, operator, target_type, target_id, before_data, after_data, remark, created_at) VALUES(?,?,?,?,?,?,?,?,NOW())",
                module,
                action,
                op,
                targetType,
                targetId,
                toJson(beforeData),
                toJson(afterData),
                remark
        );
    }

    public List<Map<String, Object>> list(String module, String operator, String keyword, String targetType, String targetId, Integer limit) {
        int finalLimit = (limit == null || limit <= 0 || limit > 500) ? 200 : limit;
        StringBuilder sql = new StringBuilder("SELECT * FROM operation_log WHERE 1=1");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (module != null && !module.isBlank()) {
            sql.append(" AND module = ?");
            args.add(module.trim());
        }
        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator = ?");
            args.add(operator.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            sql.append(" AND (target_id LIKE ? OR remark LIKE ? OR before_data LIKE ? OR after_data LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (targetType != null && !targetType.isBlank()) {
            sql.append(" AND target_type = ?");
            args.add(targetType.trim());
        }
        if (targetId != null && !targetId.isBlank()) {
            sql.append(" AND target_id = ?");
            args.add(targetId.trim());
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        args.add(finalLimit);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "系统";
        }
        return operator.trim();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }
}

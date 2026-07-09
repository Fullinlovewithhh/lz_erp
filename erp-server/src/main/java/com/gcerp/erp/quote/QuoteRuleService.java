package com.gcerp.erp.quote;

import com.gcerp.erp.audit.OperationLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class QuoteRuleService {

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;

    public QuoteRuleService(JdbcTemplate jdbcTemplate, OperationLogService operationLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.operationLogService = operationLogService;
    }

    public List<Map<String, Object>> list(String keyword, String category) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCategory = category != null && !category.isBlank();
        String normalizedCategory = hasCategory ? category.trim() : "";
        if (!hasKeyword && !hasCategory) {
            return jdbcTemplate.queryForList("SELECT * FROM quote_special_rule ORDER BY updated_at DESC, id DESC");
        }
        if (!hasKeyword) {
            return jdbcTemplate.queryForList(
                    "SELECT * FROM quote_special_rule WHERE IFNULL(rule_category,'') = ? ORDER BY updated_at DESC, id DESC",
                    normalizedCategory
            );
        }
        String like = "%" + keyword.trim() + "%";
        if (!hasCategory) {
            return jdbcTemplate.queryForList(
                    "SELECT * FROM quote_special_rule WHERE rule_name LIKE ? OR remark LIKE ? OR rule_code LIKE ? ORDER BY updated_at DESC, id DESC",
                    like, like, like
            );
        }
        return jdbcTemplate.queryForList(
                "SELECT * FROM quote_special_rule WHERE IFNULL(rule_category,'') = ? AND (rule_name LIKE ? OR remark LIKE ? OR rule_code LIKE ?) ORDER BY updated_at DESC, id DESC",
                normalizedCategory, like, like, like
        );
    }

    public Map<String, Object> create(QuoteRuleCreateRequest req, String operator) {
        String name = req.getRuleName() == null ? "" : req.getRuleName().trim();
        if (name.isBlank()) throw new IllegalArgumentException("规则名称不能为空");
        String mode = normalizeMode(req.getAdjustMode());
        BigDecimal value = req.getAdjustValue() == null ? BigDecimal.ZERO : req.getAdjustValue();
        int enabled = normalizeEnabled(req.getEnabled());
        String code = "QR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String category = req.getRuleCategory() == null ? "" : req.getRuleCategory().trim();

        jdbcTemplate.update(
                "INSERT INTO quote_special_rule(rule_code, rule_category, rule_name, adjust_mode, adjust_value, unit_desc, min_area_m2, min_charge, max_charge, enabled, remark, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                code, category, name, mode, value, req.getUnitDesc(), req.getMinAreaM2(), req.getMinCharge(), req.getMaxCharge(), enabled, req.getRemark()
        );
        Map<String, Object> created = jdbcTemplate.queryForMap("SELECT * FROM quote_special_rule WHERE rule_code = ?", code);
        operationLogService.log("报价规则", "CREATE", operator, "quote_special_rule", String.valueOf(created.get("id")), null, created, "新增报价规则");
        return created;
    }

    public Map<String, Object> update(Long id, QuoteRuleCreateRequest req, String operator) {
        String name = req.getRuleName() == null ? "" : req.getRuleName().trim();
        if (name.isBlank()) throw new IllegalArgumentException("规则名称不能为空");
        String mode = normalizeMode(req.getAdjustMode());
        BigDecimal value = req.getAdjustValue() == null ? BigDecimal.ZERO : req.getAdjustValue();
        int enabled = normalizeEnabled(req.getEnabled());
        String category = req.getRuleCategory() == null ? "" : req.getRuleCategory().trim();

        Map<String, Object> before = jdbcTemplate.queryForMap("SELECT * FROM quote_special_rule WHERE id = ?", id);
        int affected = jdbcTemplate.update(
                "UPDATE quote_special_rule SET rule_category=?, rule_name=?, adjust_mode=?, adjust_value=?, unit_desc=?, min_area_m2=?, min_charge=?, max_charge=?, enabled=?, remark=?, updated_at=NOW() WHERE id=?",
                category, name, mode, value, req.getUnitDesc(), req.getMinAreaM2(), req.getMinCharge(), req.getMaxCharge(), enabled, req.getRemark(), id
        );
        if (affected <= 0) throw new IllegalArgumentException("规则不存在");
        Map<String, Object> after = jdbcTemplate.queryForMap("SELECT * FROM quote_special_rule WHERE id = ?", id);
        operationLogService.log("报价规则", "UPDATE", operator, "quote_special_rule", String.valueOf(id), before, after, "修改报价规则");
        return after;
    }

    public int delete(Long id, String operator) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM quote_special_rule WHERE id = ?", id);
        int affected = jdbcTemplate.update("DELETE FROM quote_special_rule WHERE id = ?", id);
        if (affected > 0) {
            operationLogService.log("报价规则", "DELETE", operator, "quote_special_rule", String.valueOf(id), rows.isEmpty() ? null : rows.get(0), null, "删除报价规则");
        }
        return affected;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return "FIXED_PER_M2";
        String m = mode.trim().toUpperCase();
        if ("PERCENT".equals(m) || "FIXED_PER_ITEM".equals(m) || "FIXED_PER_M2".equals(m)) return m;
        return "FIXED_PER_M2";
    }

    private int normalizeEnabled(Integer enabled) {
        if (enabled == null) return 1;
        if (enabled == 2) return 2;
        return enabled == 0 ? 0 : 1;
    }
}


package com.gcerp.erp.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BatchImportService {

    private final JdbcTemplate jdbcTemplate;

    public BatchImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String CUSTOMER = "\u5ba2\u6237\u6863\u6848";
    private static final String STAFF = "\u4eba\u5458\u7ba1\u7406";
    private static final String PRODUCT = "\u4ea7\u54c1\u8d44\u6599";
    private static final String MATERIAL = "\u6750\u6599\u8d44\u6599";
    private static final String PROCESS_RULE = "\u5de5\u827a\u89c4\u5219";
    private static final String PRICE_RULE = "\u4ef7\u683c\u89c4\u5219";
    private static final String CONTRACT = "\u5408\u540c\u8ba2\u5355";

    private static final Map<String, String> TABLE_MAP = Map.of(
            CUSTOMER, "customer",
            STAFF, "staff",
            PRODUCT, "product",
            MATERIAL, "material",
            PROCESS_RULE, "process_rule",
            PRICE_RULE, "price_rule",
            CONTRACT, "contract"
    );

    private static final Map<String, Set<String>> ALLOW_FIELDS = Map.of(
            CUSTOMER, Set.of("customer_code", "customer_name", "phone", "address", "level", "owner", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            STAFF, Set.of("staff_code", "staff_name", "role_type", "process_name", "phone", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            PRODUCT, Set.of("product_code", "type", "product_name", "model", "material_name", "color", "handle_color", "unit_price", "unit_price_unit", "thickness", "thickness_unit", "size", "unit", "status", "image_url", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            MATERIAL, Set.of("material_code", "material_name", "material_type", "color", "length_mm", "width_mm", "thickness_mm", "unit", "status", "image_url", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            PROCESS_RULE, Set.of("rule_code", "rule_name", "rule_type", "rule_content", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            PRICE_RULE, Set.of("rule_code", "rule_name", "price_type", "price_value", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            CONTRACT, Set.of("contract_no", "customer_name", "customer_phone", "customer_address", "demand_desc", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3")
    );

    public Map<String, Set<String>> getModuleFieldMap() {
        return ALLOW_FIELDS;
    }

    public BatchImportResult importRows(String module, BatchImportRequest req) {
        String table = TABLE_MAP.get(module);
        if (table == null) {
            throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7684\u5bfc\u5165\u6a21\u5757\uff1a" + module);
        }

        Set<String> allow = ALLOW_FIELDS.get(module);
        int total = req.getRows().size();
        int success = 0;
        int failed = 0;

        for (Map<String, Object> row : req.getRows()) {
            try {
                Map<String, Object> cleaned = normalizeRow(module, row, allow);
                executeInsert(table, cleaned, req.isIgnoreDuplicate());
                success++;
            } catch (Exception ignored) {
                failed++;
            }
        }

        return new BatchImportResult(total, success, failed);
    }

    private Map<String, Object> normalizeRow(String module, Map<String, Object> row, Set<String> allow) {
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (allow.contains(e.getKey())) {
                cleaned.put(e.getKey(), normalizeValue(e.getKey(), e.getValue()));
            }
        }

        if (CONTRACT.equals(module) && !cleaned.containsKey("status")) {
            cleaned.put("status", "\u5f85\u8bbe\u8ba1");
        }
        if (!cleaned.containsKey("created_at")) {
            cleaned.put("created_at", LocalDateTime.now());
        }
        if (!cleaned.containsKey("updated_at")) {
            cleaned.put("updated_at", LocalDateTime.now());
        }
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("\u884c\u6570\u636e\u6ca1\u6709\u53ef\u5bfc\u5165\u5b57\u6bb5");
        }
        return cleaned;
    }

    private Object normalizeValue(String key, Object value) {
        if (!(value instanceof String text)) return value;
        String trimmed = text.trim();
        if (trimmed.isEmpty() || "NULL".equalsIgnoreCase(trimmed)) return null;
        if ("custom_fields".equals(key) && trimmed.isEmpty()) return null;
        return trimmed;
    }

    private void executeInsert(String table, Map<String, Object> row, boolean ignoreDuplicate) {
        List<String> columns = new ArrayList<>(row.keySet());
        String columnSql = String.join(",", columns);
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(","));
        String verb = ignoreDuplicate ? "INSERT IGNORE" : "INSERT";
        String sql = verb + " INTO " + table + " (" + columnSql + ") VALUES (" + placeholders + ")";
        Object[] args = columns.stream().map(row::get).toArray();
        jdbcTemplate.update(sql, args);
    }
}

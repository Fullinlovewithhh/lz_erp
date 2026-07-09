package com.gcerp.erp.master;

import com.gcerp.erp.audit.OperationLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MasterDataService {

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService operationLogService;

    private static final Map<String, String> MODULE_TABLE = Map.of(
            "人员管理", "staff",
            "产品资料", "product",
            "材料资料", "material"
    );

    private static final Map<String, Set<String>> UPDATE_FIELDS = Map.of(
            "人员管理", Set.of("staff_code", "staff_name", "role_type", "process_name", "phone", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            "产品资料", Set.of("product_code", "type", "product_name", "model", "material_name", "color", "handle_color", "unit_price", "unit_price_unit", "thickness", "thickness_unit", "size", "image_url", "unit", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3"),
            "材料资料", Set.of("material_code", "material_name", "material_type", "color", "length_mm", "width_mm", "thickness_mm", "image_url", "unit", "status", "custom_fields", "custom_text1", "custom_text2", "custom_text3")
    );

    public MasterDataService(JdbcTemplate jdbcTemplate, OperationLogService operationLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.operationLogService = operationLogService;
    }

    public List<Map<String, Object>> listProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) return jdbcTemplate.queryForList("SELECT * FROM product ORDER BY created_at DESC");
        return jdbcTemplate.queryForList(
                "SELECT * FROM product WHERE product_code LIKE ? OR type LIKE ? OR product_name LIKE ? OR model LIKE ? OR material_name LIKE ? OR color LIKE ? OR handle_color LIKE ? OR size LIKE ? OR status LIKE ? ORDER BY created_at DESC",
                like(keyword), like(keyword), like(keyword), like(keyword), like(keyword), like(keyword), like(keyword), like(keyword), like(keyword)
        );
    }

    public List<Map<String, Object>> listMaterials(String keyword) {
        if (keyword == null || keyword.isBlank()) return jdbcTemplate.queryForList("SELECT * FROM material ORDER BY created_at DESC");
        return jdbcTemplate.queryForList(
                "SELECT * FROM material WHERE material_name LIKE ? OR material_type LIKE ? OR color LIKE ? ORDER BY created_at DESC",
                like(keyword), like(keyword), like(keyword)
        );
    }

    public List<Map<String, Object>> listStaff(String keyword) {
        if (keyword == null || keyword.isBlank()) return jdbcTemplate.queryForList("SELECT * FROM staff ORDER BY created_at DESC");
        return jdbcTemplate.queryForList(
                "SELECT * FROM staff WHERE staff_name LIKE ? OR role_type LIKE ? OR process_name LIKE ? ORDER BY created_at DESC",
                like(keyword), like(keyword), like(keyword)
        );
    }

    public Map<String, Object> getOne(String module, Long id) {
        String table = getTable(module);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int updateOne(String module, Long id, Map<String, Object> body, String operator) {
        String table = getTable(module);
        Set<String> allow = UPDATE_FIELDS.get(module);
        Map<String, Object> before = getOne(module, id);

        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : body.entrySet()) {
            if (allow.contains(e.getKey())) cleaned.put(e.getKey(), e.getValue());
        }
        if (cleaned.isEmpty()) return 0;

        List<String> setParts = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> e : cleaned.entrySet()) {
            setParts.add(e.getKey() + " = ?");
            args.add(e.getValue());
        }
        setParts.add("updated_at = NOW()");
        args.add(id);

        String sql = "UPDATE " + table + " SET " + String.join(",", setParts) + " WHERE id = ?";
        int affected = jdbcTemplate.update(sql, args.toArray());
        if (affected > 0) {
            Map<String, Object> after = getOne(module, id);
            operationLogService.log("主数据", "UPDATE", operator, table, String.valueOf(id), before, after, "修改" + module);
        }
        return affected;
    }

    public int deleteOne(String module, Long id, String operator) {
        String table = getTable(module);
        Map<String, Object> before = getOne(module, id);
        int affected = jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", id);
        if (affected > 0) {
            operationLogService.log("主数据", "DELETE", operator, table, String.valueOf(id), before, null, "删除" + module);
        }
        return affected;
    }

    public int updateProductImage(Long id, String imageUrl) {
        return jdbcTemplate.update("UPDATE product SET image_url = ?, updated_at = NOW() WHERE id = ?", imageUrl, id);
    }

    public int updateMaterialImage(Long id, String imageUrl) {
        return jdbcTemplate.update("UPDATE material SET image_url = ?, updated_at = NOW() WHERE id = ?", imageUrl, id);
    }

    private String like(String keyword) {
        return "%" + keyword + "%";
    }

    private String getTable(String module) {
        String table = MODULE_TABLE.get(module);
        if (table == null) throw new IllegalArgumentException("不支持的模块：" + module);
        return table;
    }
}



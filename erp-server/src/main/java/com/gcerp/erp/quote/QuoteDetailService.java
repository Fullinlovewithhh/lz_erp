package com.gcerp.erp.quote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcerp.erp.audit.OperationLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuoteDetailService {

    private static final String LOG_MODULE_QUOTE_ORDER = "QUOTE_ORDER";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;
    private volatile Boolean quoteDetailHasHandleColor;
    private final Map<String, Boolean> columnExistsCache = new HashMap<>();

    public QuoteDetailService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, OperationLogService operationLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
    }

    public QuoteCalcResult calculate(QuoteCalcRequest req) {
        BigDecimal width = nz(req.getWidthMm());
        BigDecimal height = nz(req.getHeightMm());
        int qty = req.getQuantity() == null || req.getQuantity() <= 0 ? 1 : req.getQuantity();
        BigDecimal baseUnitPrice = nz(req.getBaseUnitPrice());

        BigDecimal areaM2 = width.multiply(height)
                .divide(new BigDecimal("1000000"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(qty))
                .setScale(4, RoundingMode.HALF_UP);

        List<Map<String, Object>> rules = loadRules(req.getSelectedRuleIds());
        List<Map<String, Object>> customRules = toCustomRules(req.getCustomRules());
        List<Map<String, Object>> allRules = new ArrayList<>();
        allRules.addAll(rules);
        allRules.addAll(customRules);

        BigDecimal adjustTotal = BigDecimal.ZERO;
        for (Map<String, Object> rule : allRules) {
            String mode = String.valueOf(rule.get("adjust_mode"));
            BigDecimal value = nz((BigDecimal) rule.get("adjust_value"));
            BigDecimal minArea = nz((BigDecimal) rule.get("min_area_m2"));
            BigDecimal minCharge = (BigDecimal) rule.get("min_charge");
            BigDecimal maxCharge = (BigDecimal) rule.get("max_charge");
            BigDecimal calcArea = areaM2;
            if (minArea.compareTo(BigDecimal.ZERO) > 0 && areaM2.compareTo(minArea) < 0) {
                calcArea = minArea;
            }

            BigDecimal oneRuleCharge;
            if ("PERCENT".equals(mode)) {
                oneRuleCharge = baseUnitPrice.multiply(calcArea).multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else if ("FIXED_PER_ITEM".equals(mode)) {
                BigDecimal ruleQty = nz((BigDecimal) rule.get("rule_quantity"));
                if (ruleQty.compareTo(BigDecimal.ZERO) <= 0) {
                    ruleQty = new BigDecimal(qty);
                }
                oneRuleCharge = value.multiply(ruleQty);
            } else {
                oneRuleCharge = value.multiply(calcArea);
            }
            if (minCharge != null && oneRuleCharge.compareTo(minCharge) < 0) oneRuleCharge = minCharge;
            if (maxCharge != null && oneRuleCharge.compareTo(maxCharge) > 0) oneRuleCharge = maxCharge;
            rule.put("final_charge", oneRuleCharge.setScale(2, RoundingMode.HALF_UP));
            adjustTotal = adjustTotal.add(oneRuleCharge);
        }
        adjustTotal = adjustTotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal baseAmount = baseUnitPrice.multiply(areaM2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = baseAmount.add(adjustTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalUnitPrice = areaM2.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : amount.divide(areaM2, 2, RoundingMode.HALF_UP);

        QuoteCalcResult result = new QuoteCalcResult();
        result.setAreaM2(areaM2);
        result.setSpecialAdjustTotal(adjustTotal);
        result.setFinalUnitPrice(finalUnitPrice);
        result.setAmount(amount);
        result.setAppliedRules(allRules);
        return result;
    }

    @Transactional
    public Map<String, Object> save(QuoteSaveRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("quote items are required");
        }
        BigDecimal total = BigDecimal.ZERO;
        List<QuoteCalcResult> calcResults = new ArrayList<>();
        for (QuoteCalcRequest item : req.getItems()) {
            QuoteCalcResult calc = calculate(item);
            calcResults.add(calc);
            total = total.add(calc.getAmount());
        }

        jdbcTemplate.update(
                "INSERT INTO quotation(contract_id,assignment_id,total_amount,quote_desc,created_by,created_at) VALUES(?,?,?,?,?,NOW())",
                req.getContractId(), req.getAssignmentId(), total, normalizeQuoteDesc(req.getQuoteDesc(), req.getSubmitted()), req.getOperator()
        );
        Long quoteId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        insertQuoteDetails(quoteId, req.getItems(), calcResults);

        operationLogService.log(
                LOG_MODULE_QUOTE_ORDER, "CREATE", req.getOperator(), "quotation", String.valueOf(quoteId),
                null,
                Map.of("contractId", req.getContractId(), "assignmentId", req.getAssignmentId(), "totalAmount", total, "itemCount", req.getItems().size()),
                "SAVE_QUOTE"
        );
        return Map.of("quoteId", quoteId, "totalAmount", total);
    }

    @Transactional
    public Map<String, Object> updateQuote(Long quoteId, QuoteSaveRequest req) {
        if (quoteId == null) throw new IllegalArgumentException("quoteId is required");
        if (req.getItems() == null || req.getItems().isEmpty()) throw new IllegalArgumentException("quote items are required");
        if (req.getOperator() == null || req.getOperator().isBlank()) throw new IllegalArgumentException("operator is required");

        List<Map<String, Object>> quoteRows = jdbcTemplate.queryForList("SELECT * FROM quotation WHERE id = ?", quoteId);
        if (quoteRows.isEmpty()) throw new IllegalArgumentException("quote not found");
        Map<String, Object> beforeMain = quoteRows.get(0);
        assertQuoteEditPermission(req.getOperator(), beforeMain);
        List<Map<String, Object>> beforeDetails = jdbcTemplate.queryForList("SELECT * FROM quote_detail WHERE quote_id = ? ORDER BY id ASC", quoteId);

        BigDecimal total = BigDecimal.ZERO;
        List<QuoteCalcResult> calcResults = new ArrayList<>();
        for (QuoteCalcRequest item : req.getItems()) {
            QuoteCalcResult calc = calculate(item);
            calcResults.add(calc);
            total = total.add(calc.getAmount());
        }

        jdbcTemplate.update(
                "UPDATE quotation SET contract_id=?, assignment_id=?, total_amount=?, quote_desc=? WHERE id=?",
                req.getContractId(), req.getAssignmentId(), total, normalizeQuoteDesc(req.getQuoteDesc(), req.getSubmitted()), quoteId
        );
        deleteExtraPricesByQuoteId(quoteId);
        jdbcTemplate.update("DELETE FROM quote_detail WHERE quote_id = ?", quoteId);
        insertQuoteDetails(quoteId, req.getItems(), calcResults);

        Map<String, Object> afterMain = jdbcTemplate.queryForMap("SELECT * FROM quotation WHERE id = ?", quoteId);
        List<Map<String, Object>> afterDetails = jdbcTemplate.queryForList("SELECT * FROM quote_detail WHERE quote_id = ? ORDER BY id ASC", quoteId);
        operationLogService.log(
                LOG_MODULE_QUOTE_ORDER, "UPDATE", req.getOperator(), "quotation", String.valueOf(quoteId),
                Map.of("quote", beforeMain, "details", beforeDetails),
                Map.of("quote", afterMain, "details", afterDetails),
                "UPDATE_QUOTE"
        );

        return Map.of("quoteId", quoteId, "totalAmount", total);
    }

    public List<Map<String, Object>> listByQuoteId(Long quoteId) {
        List<Map<String, Object>> details = jdbcTemplate.queryForList("SELECT * FROM quote_detail WHERE quote_id = ? ORDER BY id ASC", quoteId);
        if (details.isEmpty()) return details;
        List<Map<String, Object>> extras;
        if (hasColumn("quote_detail_extra_price", "quote_id")) {
            extras = jdbcTemplate.queryForList("SELECT * FROM quote_detail_extra_price WHERE quote_id = ? ORDER BY id ASC", quoteId);
        } else if (hasColumn("quote_detail_extra_price", "detail_id")) {
            List<Object> detailIds = details.stream()
                    .map(d -> d.get("id"))
                    .filter(v -> v instanceof Number)
                    .map(v -> ((Number) v).longValue())
                    .collect(Collectors.toList());
            if (detailIds.isEmpty()) return details;
            String placeholders = detailIds.stream().map(x -> "?").collect(Collectors.joining(","));
            extras = jdbcTemplate.queryForList(
                    "SELECT * FROM quote_detail_extra_price WHERE detail_id IN (" + placeholders + ") ORDER BY id ASC",
                    detailIds.toArray()
            );
        } else {
            extras = List.of();
        }
        Map<Long, List<Map<String, Object>>> byDetail = new LinkedHashMap<>();
        for (Map<String, Object> extra : extras) {
            Object detailId = extra.get("detail_id");
            if (detailId instanceof Number n) {
                byDetail.computeIfAbsent(n.longValue(), k -> new ArrayList<>()).add(extra);
            }
        }
        for (Map<String, Object> detail : details) {
            Object id = detail.get("id");
            if (id instanceof Number n) {
                detail.put("extra_prices", byDetail.getOrDefault(n.longValue(), List.of()));
            }
        }
        return details;
    }

    public List<Map<String, Object>> listQuotes(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        String sql = quoteListSql();
        if (kw.isEmpty()) return jdbcTemplate.queryForList(sql + " ORDER BY created_at DESC, quote_id DESC");
        String like = "%" + kw + "%";
        return jdbcTemplate.queryForList(
                sql + """
                        WHERE contract_no LIKE ?
                           OR customer_order_no LIKE ?
                           OR customer_name LIKE ?
                           OR customer_phone LIKE ?
                           OR created_by LIKE ?
                           OR engineer LIKE ?
                        ORDER BY created_at DESC, quote_id DESC
                        """,
                like, like, like, like, like, like
        );
    }

    public Map<String, Object> getQuote(Long quoteId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT q.id AS quote_id,
                       q.contract_id,
                       COALESCE(c.factory_order_no, c.contract_no) AS contract_no,
                       c.customer_order_no,
                       c.project_name_snapshot AS project_name,
                       c.customer_name,
                       c.customer_phone,
                       c.customer_address,
                       c.demand_desc,
                       c.custom_fields AS contract_custom_fields,
                       q.assignment_id,
                       qa.service_staff,
                       qa.engineer,
                       q.total_amount,
                       q.quote_desc,
                       q.created_by,
                       q.created_at
                FROM quotation q
                LEFT JOIN contract c ON c.id = q.contract_id
                LEFT JOIN quote_assignment qa ON qa.id = q.assignment_id
                WHERE q.id = ?
                """, quoteId);
        if (rows.isEmpty()) {
            rows = jdbcTemplate.queryForList("""
                    SELECT d.quote_id,
                           NULL AS contract_id,
                           CONCAT('HISTORY-QUOTE-', d.quote_id) AS contract_no,
                           NULL AS customer_order_no,
                           NULL AS project_name,
                           NULL AS customer_name,
                           NULL AS customer_phone,
                           NULL AS customer_address,
                           NULL AS demand_desc,
                           NULL AS contract_custom_fields,
                           NULL AS assignment_id,
                           NULL AS service_staff,
                           NULL AS engineer,
                           SUM(d.amount) AS total_amount,
                           'Legacy quote detail summary' AS quote_desc,
                           NULL AS created_by,
                           MIN(d.created_at) AS created_at
                    FROM quote_detail d
                    WHERE d.quote_id = ?
                    GROUP BY d.quote_id
                    """, quoteId);
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("quote not found");
        return rows.get(0);
    }

    public List<Map<String, Object>> listQuoteLogs(Long quoteId) {
        return operationLogService.list(LOG_MODULE_QUOTE_ORDER, null, null, "quotation", String.valueOf(quoteId), 300);
    }

    private String quoteListSql() {
        return """
                SELECT *
                FROM (
                    SELECT q.id AS quote_id,
                           q.contract_id,
                           COALESCE(c.factory_order_no, c.contract_no) AS contract_no,
                           c.customer_order_no,
                           c.project_name_snapshot AS project_name,
                           c.customer_name,
                           c.customer_phone,
                           c.customer_address,
                           c.demand_desc,
                           c.custom_fields AS contract_custom_fields,
                           q.assignment_id,
                           qa.service_staff,
                           qa.engineer,
                           q.total_amount,
                           q.quote_desc,
                           q.created_by,
                           q.created_at
                    FROM quotation q
                    LEFT JOIN contract c ON c.id = q.contract_id
                    LEFT JOIN quote_assignment qa ON qa.id = q.assignment_id
                    WHERE EXISTS (SELECT 1 FROM quote_detail d0 WHERE d0.quote_id = q.id)
                    UNION ALL
                    SELECT d.quote_id,
                           NULL AS contract_id,
                           CONCAT('HISTORY-QUOTE-', d.quote_id) AS contract_no,
                           NULL AS customer_order_no,
                           NULL AS project_name,
                           NULL AS customer_name,
                           NULL AS customer_phone,
                           NULL AS customer_address,
                           NULL AS demand_desc,
                           NULL AS contract_custom_fields,
                           NULL AS assignment_id,
                           NULL AS service_staff,
                           NULL AS engineer,
                           SUM(d.amount) AS total_amount,
                           'Legacy quote detail summary' AS quote_desc,
                           NULL AS created_by,
                           MIN(d.created_at) AS created_at
                    FROM quote_detail d
                    LEFT JOIN quotation q ON q.id = d.quote_id
                    WHERE d.quote_id IS NOT NULL AND q.id IS NULL
                    GROUP BY d.quote_id
                ) t
                """;
    }

    @Transactional
    public Map<String, Object> createAssignment(QuoteAssignmentRequest req) {
        if (req.getContractId() == null) throw new IllegalArgumentException("contractId is required");
        String engineer = req.getEngineer() == null ? "" : req.getEngineer().trim();
        String status = req.getStatus() == null ? "" : req.getStatus().trim();
        if (status.isBlank()) status = engineer.isBlank() ? "PENDING_CLAIM" : "ASSIGNED";
        if (engineer.isBlank() && req.getStatus() == null) throw new IllegalArgumentException("engineer is required");
        String serviceStaff = req.getServiceStaff() == null ? "" : req.getServiceStaff();
        String remark = req.getRemark() == null ? "" : req.getRemark();

        Map<String, Object> c = jdbcTemplate.queryForMap("SELECT COALESCE(factory_order_no, contract_no) AS contract_no FROM contract WHERE id = ?", req.getContractId());
        String contractNo = String.valueOf(c.getOrDefault("contract_no", ""));
        jdbcTemplate.update(
                "INSERT INTO quote_assignment(contract_id,contract_no,service_staff,engineer,status,remark,created_at,updated_at) VALUES(?,?,?,?,?,?,NOW(),NOW())",
                req.getContractId(),
                contractNo,
                serviceStaff,
                engineer,
                status,
                remark
        );
        Long assignmentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        operationLogService.log(
                "QUOTE_ASSIGNMENT", "CREATE", serviceStaff, "quote_assignment", String.valueOf(assignmentId),
                null,
                Map.of("contractId", req.getContractId(), "contractNo", contractNo, "serviceStaff", serviceStaff, "engineer", engineer, "status", status, "remark", remark),
                "CREATE_QUOTE_ASSIGNMENT"
        );
        return Map.of("assignmentId", assignmentId, "contractId", req.getContractId(), "engineer", engineer, "status", status);
    }

    @Transactional
    public Map<String, Object> updateAssignment(Long assignmentId, QuoteAssignmentRequest req) {
        if (assignmentId == null) throw new IllegalArgumentException("assignmentId is required");
        String engineer = req.getEngineer() == null ? "" : req.getEngineer().trim();
        if (engineer.isBlank()) throw new IllegalArgumentException("engineer is required");
        String status = req.getStatus() == null ? "" : req.getStatus().trim();
        if (status.isBlank()) status = "ASSIGNED";
        String serviceStaff = req.getServiceStaff() == null ? "" : req.getServiceStaff();
        String remark = req.getRemark() == null ? "" : req.getRemark();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM quote_assignment WHERE id = ?", assignmentId);
        if (rows.isEmpty()) throw new IllegalArgumentException("quote assignment not found");
        Map<String, Object> before = rows.get(0);
        jdbcTemplate.update(
                "UPDATE quote_assignment SET service_staff = ?, engineer = ?, status = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                serviceStaff,
                engineer,
                status,
                remark,
                assignmentId
        );
        Map<String, Object> after = jdbcTemplate.queryForMap("SELECT * FROM quote_assignment WHERE id = ?", assignmentId);
        operationLogService.log(
                "QUOTE_ASSIGNMENT", "UPDATE", serviceStaff, "quote_assignment", String.valueOf(assignmentId),
                before,
                after,
                "UPDATE_QUOTE_ASSIGNMENT"
        );
        return Map.of("assignmentId", assignmentId, "contractId", after.get("contract_id"), "engineer", engineer, "status", status);
    }

    public List<Map<String, Object>> listAssignments(Long contractId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        StringBuilder sql = new StringBuilder("""
                SELECT qa.id,
                       qa.contract_id,
                       COALESCE(c.factory_order_no, qa.contract_no) AS contract_no,
                       c.customer_order_no,
                       c.project_name_snapshot AS project_name,
                       qa.service_staff,
                       qa.engineer,
                       CASE WHEN COUNT(CASE WHEN q.id IS NOT NULL AND (q.quote_desc IS NULL OR q.quote_desc NOT LIKE '[DRAFT]%') THEN 1 END) > 0 THEN 'QUOTED' ELSE qa.status END AS status,
                       qa.remark,
                       qa.created_at,
                       qa.updated_at,
                       COUNT(CASE WHEN q.id IS NOT NULL AND (q.quote_desc IS NULL OR q.quote_desc NOT LIKE '[DRAFT]%') THEN 1 END) AS quote_count,
                       COALESCE(SUM(CASE WHEN q.id IS NOT NULL AND (q.quote_desc IS NULL OR q.quote_desc NOT LIKE '[DRAFT]%') THEN q.total_amount ELSE 0 END), 0) AS total_amount,
                       MAX(CASE WHEN q.id IS NOT NULL AND (q.quote_desc IS NULL OR q.quote_desc NOT LIKE '[DRAFT]%') THEN q.created_at ELSE NULL END) AS last_quote_at
                FROM quote_assignment qa
                LEFT JOIN contract c ON c.id = qa.contract_id
                LEFT JOIN quotation q ON q.assignment_id = qa.id
                    AND EXISTS (SELECT 1 FROM quote_detail d WHERE d.quote_id = q.id)
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (contractId != null) {
            sql.append(" AND qa.contract_id = ? ");
            params.add(contractId);
        }
        if (!kw.isEmpty()) {
            sql.append(" AND (COALESCE(c.factory_order_no, qa.contract_no) LIKE ? OR c.customer_order_no LIKE ? OR qa.service_staff LIKE ? OR qa.engineer LIKE ?) ");
            String like = "%" + kw + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append("""
                GROUP BY qa.id, qa.contract_id, COALESCE(c.factory_order_no, qa.contract_no), c.customer_order_no, c.project_name_snapshot, qa.service_staff, qa.engineer, qa.status, qa.remark, qa.created_at, qa.updated_at
                ORDER BY qa.created_at DESC, qa.id DESC
                """);
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    @Transactional
    public Map<String, Object> deleteQuote(Long quoteId, String operator) {
        List<Map<String, Object>> quoteRows = jdbcTemplate.queryForList("SELECT * FROM quotation WHERE id = ?", quoteId);
        List<Map<String, Object>> details = jdbcTemplate.queryForList("SELECT * FROM quote_detail WHERE quote_id = ?", quoteId);

        Integer detailCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quote_detail WHERE quote_id = ?", Integer.class, quoteId);
        deleteExtraPricesByQuoteId(quoteId);
        int deletedDetails = jdbcTemplate.update("DELETE FROM quote_detail WHERE quote_id = ?", quoteId);
        int deletedQuotes = jdbcTemplate.update("DELETE FROM quotation WHERE id = ?", quoteId);
        if (deletedQuotes == 0 && (detailCount == null || detailCount == 0)) {
            throw new IllegalArgumentException("quote not found or already deleted");
        }

        operationLogService.log(
                LOG_MODULE_QUOTE_ORDER, "DELETE", operator, "quotation", String.valueOf(quoteId),
                Map.of("quote", quoteRows, "details", details),
                null,
                "DELETE_QUOTE"
        );
        return Map.of("quoteId", quoteId, "deletedQuote", deletedQuotes, "deletedDetails", deletedDetails);
    }

    private void insertQuoteDetails(Long quoteId, List<QuoteCalcRequest> items, List<QuoteCalcResult> calcResults) {
        boolean hasHandleColor = hasQuoteDetailHandleColorColumn();
        for (int i = 0; i < items.size(); i++) {
            QuoteCalcRequest item = items.get(i);
            QuoteCalcResult calc = calcResults.get(i);
            String selectedRuleIds = item.getSelectedRuleIds() == null ? null :
                    item.getSelectedRuleIds().stream().map(String::valueOf).collect(Collectors.joining(","));
            String customJson = toJson(item.getCustomRules());
            if (hasHandleColor) {
                jdbcTemplate.update(
                        "INSERT INTO quote_detail(quote_id,product_name,material_structure,handle_color,width_mm,height_mm,thickness_mm,quantity,hinge_hole,process_desc,attachment_name,unit,area_m2,base_unit_price,special_adjust_total,final_unit_price,amount,selected_rule_ids,custom_rule_json,created_at,updated_at)" +
                                " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                        quoteId, item.getProductName(), item.getMaterialStructure(), item.getHandleColor(), nz(item.getWidthMm()), nz(item.getHeightMm()),
                        item.getThicknessMm(), item.getQuantity(), item.getHingeHole(), item.getProcessDesc(), item.getAttachmentName(),
                        item.getUnit() == null ? "m2" : item.getUnit(), calc.getAreaM2(), nz(item.getBaseUnitPrice()), calc.getSpecialAdjustTotal(),
                        calc.getFinalUnitPrice(), calc.getAmount(), selectedRuleIds, customJson
                );
            } else {
                jdbcTemplate.update(
                        "INSERT INTO quote_detail(quote_id,product_name,material_structure,width_mm,height_mm,thickness_mm,quantity,hinge_hole,process_desc,attachment_name,unit,area_m2,base_unit_price,special_adjust_total,final_unit_price,amount,selected_rule_ids,custom_rule_json,created_at,updated_at)" +
                                " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, ?,NOW(),NOW())",
                        quoteId, item.getProductName(), item.getMaterialStructure(), nz(item.getWidthMm()), nz(item.getHeightMm()),
                        item.getThicknessMm(), item.getQuantity(), item.getHingeHole(), item.getProcessDesc(), item.getAttachmentName(),
                        item.getUnit() == null ? "m2" : item.getUnit(), calc.getAreaM2(), nz(item.getBaseUnitPrice()), calc.getSpecialAdjustTotal(),
                        calc.getFinalUnitPrice(), calc.getAmount(), selectedRuleIds, customJson
                );
            }
            Long detailId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            insertExtraPrices(quoteId, detailId, calc.getAppliedRules());
        }
    }

    private boolean hasQuoteDetailHandleColorColumn() {
        if (quoteDetailHasHandleColor != null) return quoteDetailHasHandleColor;
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quote_detail' AND COLUMN_NAME = 'handle_color'",
                Integer.class
        );
        quoteDetailHasHandleColor = cnt != null && cnt > 0;
        return quoteDetailHasHandleColor;
    }

    private void insertExtraPrices(Long quoteId, Long detailId, List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return;
        final boolean hasSourceRuleId = hasColumn("quote_detail_extra_price", "source_rule_id");
        final boolean hasMinArea = hasColumn("quote_detail_extra_price", "min_area_m2");
        final boolean hasMinCharge = hasColumn("quote_detail_extra_price", "min_charge");
        final boolean hasMaxCharge = hasColumn("quote_detail_extra_price", "max_charge");
        final boolean hasRuleQty = hasColumn("quote_detail_extra_price", "rule_quantity");
        final boolean hasFinalCharge = hasColumn("quote_detail_extra_price", "final_charge");
        final boolean hasRemark = hasColumn("quote_detail_extra_price", "remark");
        for (Map<String, Object> rule : rules) {
            List<String> columns = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            columns.add("detail_id"); args.add(detailId);
            columns.add("quote_id"); args.add(quoteId);
            if (hasSourceRuleId) { columns.add("source_rule_id"); args.add(toLong(rule.get("source_rule_id"))); }
            columns.add("rule_name"); args.add(rule.get("rule_name"));
            columns.add("adjust_mode"); args.add(rule.get("adjust_mode"));
            columns.add("adjust_value"); args.add(rule.get("adjust_value"));
            columns.add("unit_desc"); args.add(rule.get("unit_desc"));
            if (hasMinArea) { columns.add("min_area_m2"); args.add(rule.get("min_area_m2")); }
            if (hasMinCharge) { columns.add("min_charge"); args.add(rule.get("min_charge")); }
            if (hasMaxCharge) { columns.add("max_charge"); args.add(rule.get("max_charge")); }
            if (hasRuleQty) { columns.add("rule_quantity"); args.add(rule.get("rule_quantity")); }
            if (hasFinalCharge) { columns.add("final_charge"); args.add(rule.get("final_charge")); }
            if (hasRemark) { columns.add("remark"); args.add(rule.get("remark")); }
            columns.add("created_at");
            columns.add("updated_at");

            StringBuilder sql = new StringBuilder("INSERT INTO quote_detail_extra_price(");
            sql.append(String.join(",", columns)).append(") VALUES(");
            List<String> holders = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) {
                String c = columns.get(i);
                if ("created_at".equals(c) || "updated_at".equals(c)) holders.add("NOW()");
                else holders.add("?");
            }
            sql.append(String.join(",", holders)).append(")");
            jdbcTemplate.update(sql.toString(), args.toArray());
        }
    }

    private int deleteExtraPricesByQuoteId(Long quoteId) {
        if (quoteId == null) return 0;
        if (hasColumn("quote_detail_extra_price", "quote_id")) {
            return jdbcTemplate.update("DELETE FROM quote_detail_extra_price WHERE quote_id = ?", quoteId);
        }
        if (!hasColumn("quote_detail_extra_price", "detail_id")) {
            return 0;
        }
        List<Map<String, Object>> detailRows = jdbcTemplate.queryForList("SELECT id FROM quote_detail WHERE quote_id = ?", quoteId);
        List<Object> detailIds = detailRows.stream()
                .map(r -> r.get("id"))
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).longValue())
                .collect(Collectors.toList());
        if (detailIds.isEmpty()) return 0;
        String placeholders = detailIds.stream().map(x -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.update("DELETE FROM quote_detail_extra_price WHERE detail_id IN (" + placeholders + ")", detailIds.toArray());
    }

    private boolean hasColumn(String table, String column) {
        String key = table + "." + column;
        Boolean cached = columnExistsCache.get(key);
        if (cached != null) return cached;
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                table,
                column
        );
        boolean exists = cnt != null && cnt > 0;
        columnExistsCache.put(key, exists);
        return exists;
    }

    private void assertQuoteEditPermission(String operator, Map<String, Object> quoteRow) {
        String op = operator == null ? "" : operator.trim();
        String createdBy = String.valueOf(quoteRow.getOrDefault("created_by", "")).trim();
        if (op.equals(createdBy)) return;

        Object assignmentIdObj = quoteRow.get("assignment_id");
        if (assignmentIdObj != null) {
            Long assignmentId = ((Number) assignmentIdObj).longValue();
            List<Map<String, Object>> assignmentRows = jdbcTemplate.queryForList(
                    "SELECT engineer FROM quote_assignment WHERE id = ?",
                    assignmentId
            );
            if (!assignmentRows.isEmpty()) {
                String engineer = String.valueOf(assignmentRows.get(0).getOrDefault("engineer", "")).trim();
                if (op.equals(engineer)) return;
            }
        }

        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff WHERE staff_name = ? AND role_type LIKE '%闂佽楠哥粻宥夊垂閸濆嫸鑰?'",
                Integer.class,
                op
        );
        if (cnt == null || cnt <= 0) {
            throw new IllegalArgumentException("only service staff, creator, or assigned engineer can edit this quote");
        }
    }

    private List<Map<String, Object>> loadRules(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
        return jdbcTemplate.queryForList(
                "SELECT id, id AS source_rule_id, rule_code, rule_name, adjust_mode, adjust_value, unit_desc, min_area_m2, min_charge, max_charge, remark FROM quote_special_rule WHERE id IN (" + placeholders + ")",
                ids.toArray()
        );
    }

    private List<Map<String, Object>> toCustomRules(List<QuoteRuleCreateRequest> list) {
        if (list == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (QuoteRuleCreateRequest r : list) {
            if (r.getRuleName() == null || r.getRuleName().isBlank()) continue;
            String mode = normalizeMode(r.getAdjustMode());
            BigDecimal value = nz(r.getAdjustValue());
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("id", r.getSourceRuleId() == null ? 0L : r.getSourceRuleId());
            one.put("source_rule_id", r.getSourceRuleId());
            one.put("rule_code", "CUSTOM");
            one.put("rule_name", r.getRuleName());
            one.put("adjust_mode", mode);
            one.put("adjust_value", value);
            one.put("unit_desc", r.getUnitDesc());
            one.put("min_area_m2", r.getMinAreaM2());
            one.put("min_charge", r.getMinCharge());
            one.put("max_charge", r.getMaxCharge());
            one.put("rule_quantity", r.getRuleQuantity());
            one.put("remark", r.getRemark() == null ? "" : r.getRemark());
            out.add(one);
        }
        return out;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) return "FIXED_PER_M2";
        String m = mode.trim().toUpperCase();
        if ("PERCENT".equals(m) || "FIXED_PER_ITEM".equals(m) || "FIXED_PER_M2".equals(m)) return m;
        return "FIXED_PER_M2";
    }

    private BigDecimal nz(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    private String normalizeQuoteDesc(String quoteDesc, Boolean submitted) {
        String raw = quoteDesc == null ? "" : quoteDesc.trim();
        boolean isSubmitted = Boolean.TRUE.equals(submitted);
        String withoutPrefix = raw.startsWith("[DRAFT]") ? raw.substring(7).trim() : raw;
        if (isSubmitted) return withoutPrefix;
        if (withoutPrefix.isEmpty()) return "[DRAFT]";
        return "[DRAFT] " + withoutPrefix;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) {
            long v = n.longValue();
            return v == 0L ? null : v;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "0".equals(text)) return null;
        return Long.parseLong(text);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}


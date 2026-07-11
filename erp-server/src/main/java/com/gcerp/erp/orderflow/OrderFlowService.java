package com.gcerp.erp.orderflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcerp.erp.auth.AuthContext;
import com.gcerp.erp.auth.CurrentUser;
import com.gcerp.erp.quote.QuoteCalcRequest;
import com.gcerp.erp.quote.QuoteCalcResult;
import com.gcerp.erp.quote.QuoteDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderFlowService {
    private final JdbcTemplate jdbcTemplate;
    private final QuoteDetailService quoteDetailService;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> listProductionLines(Boolean enabled) {
        if (enabled == null) {
            return jdbcTemplate.queryForList("SELECT * FROM production_line ORDER BY sort_order,id");
        }
        return jdbcTemplate.queryForList("SELECT * FROM production_line WHERE enabled=? ORDER BY sort_order,id", enabled ? 1 : 0);
    }

    @Transactional
    public Map<String, Object> createProductionLine(OrderFlowController.ProductionLineRequest req) {
        requireAdmin();
        String code = normalizeLineCode(req.lineCode());
        String name = required(req.lineName(), "生产线名称不能为空");
        jdbcTemplate.update("""
                INSERT INTO production_line(line_code,line_name,enabled,sort_order,created_at,updated_at)
                VALUES(?,?,?,?,NOW(),NOW())
                """, code, name, Boolean.FALSE.equals(req.enabled()) ? 0 : 1, nvl(req.sortOrder(), 0));
        return jdbcTemplate.queryForMap("SELECT * FROM production_line WHERE line_code=?", code);
    }

    @Transactional
    public Map<String, Object> updateProductionLine(Long id, OrderFlowController.ProductionLineRequest req) {
        requireAdmin();
        requireExists("production_line", "id", id);
        jdbcTemplate.update("""
                UPDATE production_line SET line_code=?,line_name=?,enabled=?,sort_order=?,updated_at=NOW() WHERE id=?
                """, normalizeLineCode(req.lineCode()), required(req.lineName(), "生产线名称不能为空"),
                Boolean.FALSE.equals(req.enabled()) ? 0 : 1, nvl(req.sortOrder(), 0), id);
        return jdbcTemplate.queryForMap("SELECT * FROM production_line WHERE id=?", id);
    }

    public List<Map<String, Object>> listReceivingAccounts(Boolean enabled) {
        if (enabled == null) return jdbcTemplate.queryForList("SELECT * FROM receiving_account ORDER BY sort_order,id");
        return jdbcTemplate.queryForList("SELECT * FROM receiving_account WHERE enabled=? ORDER BY sort_order,id", enabled ? 1 : 0);
    }

    @Transactional
    public Map<String, Object> createReceivingAccount(OrderFlowController.ReceivingAccountRequest req) {
        requireRole("FINANCE", "ADMIN");
        String code = required(req.accountCode(), "收款账户编码不能为空").toUpperCase(Locale.ROOT);
        jdbcTemplate.update("""
                INSERT INTO receiving_account(account_code,account_name,payment_method,bank_name,account_no_masked,
                enabled,sort_order,created_at,updated_at) VALUES(?,?,?,?,?,?,?,NOW(),NOW())
                """, code, required(req.accountName(), "收款账户名称不能为空"),
                required(req.paymentMethod(), "付款方式不能为空"), trim(req.bankName()), trim(req.accountNoMasked()),
                Boolean.FALSE.equals(req.enabled()) ? 0 : 1, nvl(req.sortOrder(), 0));
        return jdbcTemplate.queryForMap("SELECT * FROM receiving_account WHERE account_code=?", code);
    }

    @Transactional
    public Map<String, Object> updateReceivingAccount(Long id, OrderFlowController.ReceivingAccountRequest req) {
        requireRole("FINANCE", "ADMIN");
        requireExists("receiving_account", "id", id);
        jdbcTemplate.update("""
                UPDATE receiving_account SET account_code=?,account_name=?,payment_method=?,bank_name=?,
                account_no_masked=?,enabled=?,sort_order=?,updated_at=NOW() WHERE id=?
                """, required(req.accountCode(), "收款账户编码不能为空").toUpperCase(Locale.ROOT),
                required(req.accountName(), "收款账户名称不能为空"), required(req.paymentMethod(), "付款方式不能为空"),
                trim(req.bankName()), trim(req.accountNoMasked()), Boolean.FALSE.equals(req.enabled()) ? 0 : 1,
                nvl(req.sortOrder(), 0), id);
        return jdbcTemplate.queryForMap("SELECT * FROM receiving_account WHERE id=?", id);
    }

    public List<Map<String, Object>> listHardwareItems(String keyword) {
        String kw = trim(keyword);
        String like = kw == null ? "%%" : "%" + kw + "%";
        return jdbcTemplate.queryForList("""
                SELECT hi.*,hs.on_hand_quantity,hs.reserved_quantity,hs.available_quantity,hs.min_stock_quantity
                FROM hardware_item hi LEFT JOIN hardware_stock hs ON hs.hardware_item_id=hi.id
                WHERE hi.hardware_code LIKE ? OR hi.hardware_name LIKE ? OR COALESCE(hi.specification,'') LIKE ?
                ORDER BY hi.enabled DESC,hi.hardware_code
                """, like, like, like);
    }

    @Transactional
    public Map<String, Object> createHardwareItem(OrderFlowController.HardwareItemRequest req) {
        requireRole("ADMIN", "SERVICE", "ENGINEER");
        String code = required(req.hardwareCode(), "五金编码不能为空").toUpperCase(Locale.ROOT);
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO hardware_item(hardware_code,hardware_name,specification,unit,sale_price,stock_mode,enabled,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,NOW(),NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code); ps.setString(2, required(req.hardwareName(), "五金名称不能为空"));
            ps.setString(3, trim(req.specification())); ps.setString(4, required(req.unit(), "单位不能为空"));
            ps.setBigDecimal(5, nonNegative(req.salePrice(), "销售价格不能小于0"));
            ps.setString(6, req.stockMode() == null ? "STOCK" : req.stockMode().toUpperCase(Locale.ROOT));
            ps.setInt(7, Boolean.FALSE.equals(req.enabled()) ? 0 : 1); return ps;
        }, key);
        long id = key.getKey().longValue();
        jdbcTemplate.update("""
                INSERT INTO hardware_stock(hardware_item_id,on_hand_quantity,reserved_quantity,available_quantity,min_stock_quantity,updated_at)
                VALUES(?,0,0,0,0,NOW())
                """, id);
        return jdbcTemplate.queryForMap("SELECT * FROM hardware_item WHERE id=?", id);
    }

    public List<Map<String, Object>> listReviewPool() {
        return jdbcTemplate.queryForList("""
                SELECT co.*, c.customer_name, p.project_name,
                       u.display_name AS review_engineer_name
                FROM customer_order co
                JOIN customer c ON c.id=co.customer_id
                LEFT JOIN project p ON p.project_id=co.project_id
                LEFT JOIN app_user u ON u.id=co.review_engineer_id
                WHERE co.is_deleted=0 AND co.review_status IN ('待领取','评审中')
                ORDER BY co.created_at
                """);
    }

    @Transactional
    public Map<String, Object> claimReview(Long customerOrderId) {
        requireRole("ENGINEER", "ADMIN");
        int changed = jdbcTemplate.update("""
                UPDATE customer_order SET review_engineer_id=?,review_status='评审中',updated_at=NOW()
                WHERE id=? AND review_engineer_id IS NULL AND is_deleted=0
                """, userId(), customerOrderId);
        if (changed == 0) throw new IllegalArgumentException("客户订单已被领取或不存在");
        return customerOrder(customerOrderId);
    }

    @Transactional
    public Map<String, Object> addSplitDraft(Long customerOrderId, OrderFlowController.SplitDraftRequest req) {
        Map<String, Object> order = customerOrder(customerOrderId);
        Long reviewer = longValue(order.get("review_engineer_id"));
        if (!isAdmin() && (reviewer == null || !reviewer.equals(userId()))) {
            throw new IllegalArgumentException("只有领取该CAD评审的深化设计师可以填写拆单草稿");
        }
        String type = normalizeOrderType(req.orderType());
        String parent = trim(req.parentFactoryOrderId());
        if ("SUPPLEMENT".equals(type) && parent == null) throw new IllegalArgumentException("补单必须关联原工厂订单");
        requireExists("production_line", "id", req.productionLineId());
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO customer_order_split_draft(customer_order_id,factory_order_name,production_line_id,
                    order_type,parent_factory_order_id,remark,sort_order,created_by,status,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,?, '草稿',NOW(),NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerOrderId);
            ps.setString(2, required(req.factoryOrderName(), "工厂订单名称不能为空"));
            ps.setLong(3, req.productionLineId());
            ps.setString(4, type);
            ps.setString(5, parent);
            ps.setString(6, trim(req.remark()));
            ps.setInt(7, nvl(req.sortOrder(), 0));
            ps.setLong(8, userId());
            return ps;
        }, key);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_order_split_draft WHERE id=?", key.getKey().longValue());
    }

    public List<Map<String, Object>> listSplitDrafts(Long customerOrderId) {
        return jdbcTemplate.queryForList("""
                SELECT d.*,pl.line_code,pl.line_name,u.display_name AS created_by_name
                FROM customer_order_split_draft d
                JOIN production_line pl ON pl.id=d.production_line_id
                LEFT JOIN app_user u ON u.id=d.created_by
                WHERE d.customer_order_id=? ORDER BY d.sort_order,d.id
                """, customerOrderId);
    }

    @Transactional
    public synchronized List<Map<String, Object>> confirmSplit(Long customerOrderId) {
        requireRole("SERVICE", "ADMIN");
        customerOrder(customerOrderId);
        List<Map<String, Object>> drafts = jdbcTemplate.queryForList("""
                SELECT d.*,pl.line_code FROM customer_order_split_draft d
                JOIN production_line pl ON pl.id=d.production_line_id
                WHERE d.customer_order_id=? AND d.status='草稿' ORDER BY d.sort_order,d.id
                """, customerOrderId);
        if (drafts.isEmpty()) throw new IllegalArgumentException("没有可确认的拆单草稿");
        String paymentStatus = String.valueOf(customerOrder(customerOrderId).get("payment_status"));
        boolean hasSupplement = drafts.stream().anyMatch(d -> "SUPPLEMENT".equals(String.valueOf(d.get("order_type"))));
        Integer maxSeq = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(split_sequence),0) FROM factory_order WHERE customer_order_id=?", Integer.class, customerOrderId);
        int seq = maxSeq == null ? 0 : maxSeq;
        for (Map<String, Object> draft : drafts) {
            seq++;
            String type = String.valueOf(draft.get("order_type"));
            String id = generateFactoryOrderId(String.valueOf(draft.get("line_code")), "SUPPLEMENT".equals(type));
            jdbcTemplate.update("""
                    INSERT INTO factory_order(factory_order_id,customer_order_id,production_line_id,factory_order_name,
                    order_type,parent_factory_order_id,split_sequence,status,supplement_credit_flag,demand_desc,created_by,created_at,updated_at)
                    VALUES(?,?,?,?,?,?,?,'待报价分配',?,?,?,NOW(),NOW())
                    """, id, customerOrderId, draft.get("production_line_id"), draft.get("factory_order_name"),
                    type, draft.get("parent_factory_order_id"), seq,
                    "SUPPLEMENT".equals(type) && !"财务确认全款".equals(paymentStatus) ? 1 : 0,
                    draft.get("remark"), userId());
            jdbcTemplate.update("UPDATE customer_order_split_draft SET status='已确认',updated_at=NOW() WHERE id=?", draft.get("id"));
        }
        jdbcTemplate.update("""
                UPDATE customer_order SET review_status='拆单已确认',quote_status='待报价',
                cutting_release_status=?,updated_at=NOW() WHERE id=?
                """, hasSupplement ? "补单待确认" : "未申请", customerOrderId);
        return listFactoryOrders(customerOrderId, null);
    }

    public List<Map<String, Object>> listFactoryOrders(Long customerOrderId, String status) {
        return jdbcTemplate.queryForList("""
                SELECT fo.*,co.customer_order_no,co.customer_order_name,c.customer_name,pl.line_code,pl.line_name,
                       u.display_name AS quote_assignee_name
                FROM factory_order fo
                JOIN customer_order co ON co.id=fo.customer_order_id
                JOIN customer c ON c.id=co.customer_id
                JOIN production_line pl ON pl.id=fo.production_line_id
                LEFT JOIN app_user u ON u.id=fo.quote_assignee_id
                WHERE (? IS NULL OR fo.customer_order_id=?) AND (? IS NULL OR ?='' OR fo.status=?)
                ORDER BY fo.created_at DESC
                """, customerOrderId, customerOrderId, status, status, status);
    }

    @Transactional
    public Map<String, Object> assignQuoteOwner(String factoryOrderId, Long targetUserId, String reason, boolean claim) {
        Map<String, Object> order = factoryOrder(factoryOrderId);
        Long oldUser = longValue(order.get("quote_assignee_id"));
        Long target = claim ? userId() : targetUserId;
        if (target == null) throw new IllegalArgumentException("报价负责人不能为空");
        if (claim) {
            requireRole("ENGINEER", "SERVICE", "ADMIN");
            if (oldUser != null) throw new IllegalArgumentException("该工厂订单已有报价负责人");
        } else {
            requireRole("SERVICE", "ADMIN");
        }
        Map<String, Object> targetUser = jdbcTemplate.queryForMap("SELECT id,role_code FROM app_user WHERE id=? AND enabled=1", target);
        String targetRole = String.valueOf(targetUser.get("role_code"));
        if (!List.of("ENGINEER", "SERVICE", "ADMIN").contains(targetRole.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("目标用户没有报价权限");
        }
        jdbcTemplate.update("""
                UPDATE factory_order SET quote_assignee_id=?,quote_assignee_role=?,status='报价中',updated_at=NOW()
                WHERE factory_order_id=?
                """, target, targetRole, factoryOrderId);
        jdbcTemplate.update("""
                INSERT INTO factory_order_assignment_log(factory_order_id,from_user_id,to_user_id,action_type,reason,operated_by,created_at)
                VALUES(?,?,?,?,?,?,NOW())
                """, factoryOrderId, oldUser, target, oldUser == null ? (claim ? "CLAIM" : "ASSIGN") : "TRANSFER",
                trim(reason), userId());
        return factoryOrder(factoryOrderId);
    }

    @Transactional
    public Map<String, Object> saveQuote(String factoryOrderId, OrderFlowController.QuoteRequest req) {
        Map<String, Object> order = factoryOrder(factoryOrderId);
        Long assignee = longValue(order.get("quote_assignee_id"));
        if (!isAdmin() && (assignee == null || !assignee.equals(userId()))) throw new IllegalArgumentException("只有当前报价负责人可以保存报价");
        if (req.items() == null || req.items().isEmpty()) throw new IllegalArgumentException("报价明细不能为空");
        BigDecimal rate = req.discountRate() == null ? BigDecimal.ONE : req.discountRate();
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("折扣率必须在0到1之间");
        BigDecimal original = BigDecimal.ZERO;
        BigDecimal finalAmount = BigDecimal.ZERO;
        List<QuoteCalcResult> calculations = new java.util.ArrayList<>();
        List<BigDecimal> itemFinalAmounts = new java.util.ArrayList<>();
        for (QuoteCalcRequest item : req.items()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) throw new IllegalArgumentException("数量必须大于0");
            nonNegative(item.getBaseUnitPrice(), "单价不能小于0");
            boolean hardware = isHardware(item.getProductCategory());
            QuoteCalcResult calc = hardware ? calculateHardware(item) : quoteDetailService.calculate(item);
            calculations.add(calc);
            BigDecimal baseAmount = item.getBaseUnitPrice().multiply(calc.getAreaM2()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountedBase = !hardware && !Boolean.FALSE.equals(item.getDiscountEligible())
                    ? baseAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP) : baseAmount;
            BigDecimal discountedExtras = BigDecimal.ZERO;
            for (Map<String, Object> applied : calc.getAppliedRules()) {
                BigDecimal charge = decimal(applied.get("final_charge"));
                discountedExtras = discountedExtras.add(isRuleDiscountEligible(item, applied)
                        ? charge.multiply(rate).setScale(2, RoundingMode.HALF_UP) : charge);
            }
            BigDecimal oneFinal = discountedBase.add(discountedExtras).setScale(2, RoundingMode.HALF_UP);
            original = original.add(calc.getAmount());
            finalAmount = finalAmount.add(oneFinal);
            itemFinalAmounts.add(oneFinal);
        }
        Integer current = jdbcTemplate.queryForObject("SELECT current_quote_version FROM factory_order WHERE factory_order_id=?", Integer.class, factoryOrderId);
        int version = nvl(current, 0) + 1;
        BigDecimal discountAmount = original.subtract(finalAmount);
        List<Map<String, Object>> latestRows = jdbcTemplate.queryForList("""
                SELECT * FROM factory_order_quote WHERE factory_order_id=? ORDER BY version_no DESC LIMIT 1
                """, factoryOrderId);
        boolean reuseDraft = !latestRows.isEmpty() && "草稿".equals(String.valueOf(latestRows.getFirst().get("status")));
        long quoteId;
        if (reuseDraft) {
            Map<String, Object> latest = latestRows.getFirst();
            quoteId = longValue(latest.get("id"));
            version = Integer.parseInt(String.valueOf(latest.get("version_no")));
            jdbcTemplate.update("DELETE FROM factory_order_quote_item_extra_price WHERE quote_id=?", quoteId);
            jdbcTemplate.update("DELETE FROM factory_order_quote_item WHERE quote_id=?", quoteId);
            jdbcTemplate.update("""
                    UPDATE factory_order_quote SET status=?,original_amount=?,discount_rate=?,discount_amount=?,
                    final_amount=?,quote_desc=?,submitted_at=? WHERE id=?
                    """, Boolean.TRUE.equals(req.submit()) ? "已提交" : "草稿", original, rate, discountAmount,
                    finalAmount, trim(req.quoteDesc()), Boolean.TRUE.equals(req.submit()) ? LocalDateTime.now() : null, quoteId);
        } else {
            KeyHolder key = new GeneratedKeyHolder();
            int newVersion = version;
            BigDecimal quoteOriginal = original;
            BigDecimal quoteFinal = finalAmount;
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO factory_order_quote(factory_order_id,version_no,status,original_amount,discount_rate,
                        discount_amount,final_amount,quote_desc,created_by,created_at,submitted_at)
                        VALUES(?,?,?,?,?,?,?,?,?,NOW(),?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, factoryOrderId); ps.setInt(2, newVersion);
                ps.setString(3, Boolean.TRUE.equals(req.submit()) ? "已提交" : "草稿");
                ps.setBigDecimal(4, quoteOriginal); ps.setBigDecimal(5, rate); ps.setBigDecimal(6, discountAmount);
                ps.setBigDecimal(7, quoteFinal); ps.setString(8, trim(req.quoteDesc())); ps.setLong(9, userId());
                ps.setObject(10, Boolean.TRUE.equals(req.submit()) ? LocalDateTime.now() : null);
                return ps;
            }, key);
            quoteId = key.getKey().longValue();
        }
        for (int index = 0; index < req.items().size(); index++) {
            QuoteCalcRequest item = req.items().get(index);
            QuoteCalcResult calc = calculations.get(index);
            BigDecimal itemFinal = itemFinalAmounts.get(index);
            boolean hardware = isHardware(item.getProductCategory());
            int sortOrder = index + 1;
            KeyHolder itemKey = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO factory_order_quote_item(quote_id,product_category,product_id,hardware_item_id,product_name,
                        specification,material_structure,handle_color,width_mm,height_mm,thickness_mm,hinge_hole,process_desc,
                        attachment_name,attachment_path,quantity,unit,unit_price,area_m2,base_unit_price,special_adjust_total,
                        final_unit_price,original_amount,discount_eligible,discount_amount,final_amount,selected_rule_ids,
                        custom_rule_json,production_process,technician,remark,sort_order)
                        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """, Statement.RETURN_GENERATED_KEYS);
                int p = 1;
                ps.setLong(p++, quoteId); ps.setString(p++, required(item.getProductCategory(), "产品分类不能为空"));
                ps.setObject(p++, item.getProductId()); ps.setObject(p++, item.getHardwareItemId());
                ps.setString(p++, required(item.getProductName(), "产品名称不能为空")); ps.setString(p++, trim(item.getSpecification()));
                ps.setString(p++, trim(item.getMaterialStructure())); ps.setString(p++, trim(item.getHandleColor()));
                ps.setBigDecimal(p++, item.getWidthMm()); ps.setBigDecimal(p++, item.getHeightMm()); ps.setBigDecimal(p++, item.getThicknessMm());
                ps.setString(p++, trim(item.getHingeHole())); ps.setString(p++, trim(item.getProcessDesc()));
                ps.setString(p++, trim(item.getAttachmentName())); ps.setString(p++, trim(item.getAttachmentPath()));
                ps.setInt(p++, item.getQuantity()); ps.setString(p++, item.getUnit() == null ? "m2" : item.getUnit());
                ps.setBigDecimal(p++, item.getBaseUnitPrice()); ps.setBigDecimal(p++, calc.getAreaM2());
                ps.setBigDecimal(p++, item.getBaseUnitPrice()); ps.setBigDecimal(p++, calc.getSpecialAdjustTotal());
                ps.setBigDecimal(p++, calc.getFinalUnitPrice()); ps.setBigDecimal(p++, calc.getAmount());
                ps.setInt(p++, !hardware && !Boolean.FALSE.equals(item.getDiscountEligible()) ? 1 : 0);
                ps.setBigDecimal(p++, calc.getAmount().subtract(itemFinal)); ps.setBigDecimal(p++, itemFinal);
                ps.setString(p++, joinIds(item.getSelectedRuleIds())); ps.setString(p++, json(item.getCustomRules()));
                ps.setString(p++, trim(item.getProductionProcess())); ps.setString(p++, trim(item.getTechnician()));
                ps.setString(p++, trim(item.getProcessDesc())); ps.setInt(p, sortOrder);
                return ps;
            }, itemKey);
            long quoteItemId = itemKey.getKey().longValue();
            for (Map<String, Object> applied : calc.getAppliedRules()) {
                jdbcTemplate.update("""
                        INSERT INTO factory_order_quote_item_extra_price(quote_item_id,quote_id,source_rule_id,rule_name,
                        adjust_mode,adjust_value,unit_desc,rule_quantity,final_charge,discount_eligible,created_at)
                        VALUES(?,?,?,?,?,?,?,?,?,?,NOW())
                        """, quoteItemId, quoteId, nullableLong(applied.get("source_rule_id")), applied.get("rule_name"),
                        applied.get("adjust_mode"), applied.get("adjust_value"), applied.get("unit_desc"),
                        applied.get("rule_quantity"), applied.get("final_charge"), isRuleDiscountEligible(item, applied) ? 1 : 0);
            }
        }
        Long customerOrderId = longValue(order.get("customer_order_id"));
        if (Boolean.TRUE.equals(req.submit())) {
            jdbcTemplate.update("""
                    UPDATE factory_order SET original_quote_amount=?,discount_rate=?,discounted_quote_amount=?,
                    current_quote_version=?,status='报价已提交',updated_at=NOW() WHERE factory_order_id=?
                    """, original, rate, finalAmount, version, factoryOrderId);
            jdbcTemplate.update("UPDATE customer_quote_confirmation SET status='已失效' WHERE customer_order_id=? AND status='有效'", customerOrderId);
            jdbcTemplate.update("UPDATE customer_quote_pdf SET status='已失效',invalidated_at=NOW() WHERE customer_order_id=? AND status<>'已失效'", customerOrderId);
            refreshCustomerOrderQuote(customerOrderId);
        } else {
            jdbcTemplate.update("UPDATE factory_order SET status='报价中',updated_at=NOW() WHERE factory_order_id=?", factoryOrderId);
        }
        return quoteWithItems(quoteId);
    }

    public List<Map<String, Object>> listQuotes(String factoryOrderId) {
        return jdbcTemplate.queryForList("""
                SELECT q.*,u.display_name AS created_by_name FROM factory_order_quote q
                LEFT JOIN app_user u ON u.id=q.created_by WHERE q.factory_order_id=? ORDER BY q.version_no DESC
                """, factoryOrderId);
    }

    public Map<String, Object> getQuote(Long quoteId) {
        return quoteWithItems(quoteId);
    }

    @Transactional
    public Map<String, Object> requestPriceAdjustment(Long customerOrderId, OrderFlowController.PriceAdjustmentRequest req) {
        requireRole("SERVICE", "ADMIN");
        Map<String, Object> order = customerOrder(customerOrderId);
        BigDecimal before = decimal(order.get("quote_total_amount"));
        BigDecimal after = nonNegative(req.finalAmount(), "最终成交价不能小于0");
        Integer version = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version_no),0)+1 FROM customer_order_price_adjustment WHERE customer_order_id=?
                """, Integer.class, customerOrderId);
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO customer_order_price_adjustment(customer_order_id,version_no,before_amount,after_amount,
                    adjustment_amount,reason,status,requested_by,requested_at) VALUES(?,?,?,?,?,?,'待厂长审批',?,NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerOrderId); ps.setInt(2, nvl(version, 1)); ps.setBigDecimal(3, before);
            ps.setBigDecimal(4, after); ps.setBigDecimal(5, after.subtract(before));
            ps.setString(6, required(req.reason(), "价格调整原因不能为空")); ps.setLong(7, userId());
            return ps;
        }, key);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_order_price_adjustment WHERE id=?", key.getKey().longValue());
    }

    @Transactional
    public Map<String, Object> approvePriceAdjustment(Long adjustmentId, OrderFlowController.ApprovalRequest req) {
        requireDirector();
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM customer_order_price_adjustment WHERE id=?", adjustmentId);
        String status = Boolean.TRUE.equals(req.approved()) ? "已批准" : "已驳回";
        jdbcTemplate.update("""
                UPDATE customer_order_price_adjustment SET status=?,approved_by=?,approved_at=NOW(),approval_remark=? WHERE id=?
                """, status, userId(), trim(req.remark()), adjustmentId);
        if (Boolean.TRUE.equals(req.approved())) {
            jdbcTemplate.update("""
                    UPDATE customer_order SET price_adjustment_amount=?,final_receivable_amount=?,quote_status='待客户确认',updated_at=NOW()
                    WHERE id=?
                    """, row.get("adjustment_amount"), row.get("after_amount"), row.get("customer_order_id"));
            jdbcTemplate.update("UPDATE customer_quote_confirmation SET status='已失效' WHERE customer_order_id=? AND status='有效'", row.get("customer_order_id"));
            jdbcTemplate.update("UPDATE customer_quote_pdf SET status='已失效',invalidated_at=NOW() WHERE customer_order_id=? AND status<>'已失效'", row.get("customer_order_id"));
            refreshPaymentStatus(longValue(row.get("customer_order_id")));
        }
        return jdbcTemplate.queryForMap("SELECT * FROM customer_order_price_adjustment WHERE id=?", adjustmentId);
    }

    @Transactional
    public Map<String, Object> confirmCustomerQuote(Long customerOrderId, OrderFlowController.QuoteConfirmationRequest req) {
        requireRole("SERVICE", "ADMIN");
        customerOrder(customerOrderId);
        if (req.pdfId() == null) throw new IllegalArgumentException("请先选择发送给客户的PDF版本");
        Map<String, Object> pdf = jdbcTemplate.queryForMap("SELECT * FROM customer_quote_pdf WHERE id=? AND customer_order_id=?", req.pdfId(), customerOrderId);
        if ("已失效".equals(String.valueOf(pdf.get("status")))) throw new IllegalArgumentException("该PDF版本已经失效");
        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM customer_order_price_adjustment WHERE customer_order_id=? AND status='待厂长审批'
                """, Integer.class, customerOrderId);
        if (pending != null && pending > 0) throw new IllegalArgumentException("仍有价格调整等待厂长审批");
        Integer version = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(confirmation_version),0)+1 FROM customer_quote_confirmation WHERE customer_order_id=?
                """, Integer.class, customerOrderId);
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO customer_quote_confirmation(customer_order_id,pdf_id,confirmation_version,confirmation_method,
                    confirmed_at,customer_contact,confirmation_remark,attachment_path,recorded_by,created_at)
                    VALUES(?,?,?,?,?,?,?,?,?,NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerOrderId); ps.setLong(2, req.pdfId());
            ps.setInt(3, Integer.parseInt(String.valueOf(pdf.get("pdf_version"))));
            ps.setString(4, required(req.confirmationMethod(), "确认方式不能为空"));
            ps.setObject(5, req.confirmedAt() == null ? LocalDateTime.now() : req.confirmedAt());
            ps.setString(6, required(req.customerContact(), "客户联系人不能为空"));
            ps.setString(7, trim(req.confirmationRemark())); ps.setString(8, trim(req.attachmentPath()));
            ps.setLong(9, userId()); return ps;
        }, key);
        jdbcTemplate.update("UPDATE customer_quote_pdf SET status='客户已确认' WHERE id=?", req.pdfId());
        jdbcTemplate.update("UPDATE customer_order SET quote_status='客户已确认',updated_at=NOW() WHERE id=?", customerOrderId);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_quote_confirmation WHERE id=?", key.getKey().longValue());
    }

    @Transactional
    public List<Map<String, Object>> createPaymentPlan(Long customerOrderId, OrderFlowController.PaymentPlanRequest req) {
        requireRole("SERVICE", "ADMIN");
        if (req.installments() == null || req.installments().isEmpty()) throw new IllegalArgumentException("付款计划不能为空");
        String settlementType = required(req.settlementType(), "结算类型不能为空");
        Integer version = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(plan_version),0)+1 FROM customer_payment_plan WHERE customer_order_id=?
                """, Integer.class, customerOrderId);
        int v = nvl(version, 1);
        for (OrderFlowController.PaymentInstallmentRequest item : req.installments()) {
            jdbcTemplate.update("""
                    INSERT INTO customer_payment_plan(customer_order_id,plan_version,installment_no,planned_amount,due_date,
                    status,customer_confirmed,requested_by,created_at,updated_at)
                    VALUES(?,?,?,?,?,'待厂长审批',?,?,NOW(),NOW())
                    """, customerOrderId, v, item.installmentNo(), positive(item.plannedAmount(), "计划金额必须大于0"),
                    item.dueDate(), Boolean.TRUE.equals(req.customerConfirmed()) ? 1 : 0, userId());
        }
        jdbcTemplate.update("UPDATE customer_order SET settlement_type=?,payment_status='待付款计划审批',updated_at=NOW() WHERE id=?",
                settlementType, customerOrderId);
        if ("MONTHLY".equalsIgnoreCase(settlementType)) {
            Long customerId = longValue(customerOrder(customerOrderId).get("customer_id"));
            jdbcTemplate.update("""
                    INSERT INTO customer_monthly_account(customer_id,enabled,overdue_locked,updated_at)
                    VALUES(?,1,0,NOW()) ON DUPLICATE KEY UPDATE enabled=1,updated_at=NOW()
                    """, customerId);
        }
        return jdbcTemplate.queryForList("SELECT * FROM customer_payment_plan WHERE customer_order_id=? AND plan_version=? ORDER BY installment_no", customerOrderId, v);
    }

    @Transactional
    public void approvePaymentPlan(Long customerOrderId, OrderFlowController.ApprovalRequest req) {
        requireDirector();
        Integer version = jdbcTemplate.queryForObject("SELECT MAX(plan_version) FROM customer_payment_plan WHERE customer_order_id=?", Integer.class, customerOrderId);
        if (version == null) throw new IllegalArgumentException("付款计划不存在");
        String status = Boolean.TRUE.equals(req.approved()) ? "已批准" : "已驳回";
        jdbcTemplate.update("""
                UPDATE customer_payment_plan SET status=?,approved_by=?,approved_at=NOW(),approval_remark=?,updated_at=NOW()
                WHERE customer_order_id=? AND plan_version=?
                """, status, userId(), trim(req.remark()), customerOrderId, version);
        jdbcTemplate.update("UPDATE customer_order SET payment_status=?,updated_at=NOW() WHERE id=?",
                Boolean.TRUE.equals(req.approved()) ? "付款计划已确认" : "付款计划已驳回", customerOrderId);
    }

    public List<Map<String, Object>> listPaymentReminders() {
        refreshMonthlyOverdueLocks();
        return jdbcTemplate.queryForList("""
                SELECT pp.*,co.customer_order_no,co.customer_order_name,c.customer_name,
                       DATEDIFF(pp.due_date,CURDATE()) AS days_until_due
                FROM customer_payment_plan pp
                JOIN customer_order co ON co.id=pp.customer_order_id
                JOIN customer c ON c.id=co.customer_id
                WHERE pp.status='已批准' AND pp.due_date<=DATE_ADD(CURDATE(),INTERVAL 3 DAY)
                  AND pp.planned_amount>(SELECT COALESCE(SUM(r.actual_amount),0) FROM customer_payment_receipt r
                                         WHERE r.payment_plan_id=pp.id AND r.status='财务已确认')
                ORDER BY pp.due_date
                """);
    }

    public List<Map<String, Object>> listCommercialOrders() {
        return jdbcTemplate.queryForList("""
                SELECT co.id,co.customer_order_no,co.customer_order_name,co.settlement_type,co.quote_status,
                       co.quote_total_amount,co.price_adjustment_amount,co.final_receivable_amount,
                       co.payment_status,co.cutting_release_status,c.customer_name,
                       (SELECT COALESCE(SUM(r.actual_amount),0) FROM customer_payment_receipt r
                        WHERE r.customer_order_id=co.id AND r.status='财务已确认') AS confirmed_received_amount
                FROM customer_order co JOIN customer c ON c.id=co.customer_id
                WHERE co.is_deleted=0 ORDER BY co.updated_at DESC
                """);
    }

    public Map<String, Object> commercialDetail(Long customerOrderId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", customerOrder(customerOrderId));
        result.put("factoryOrders", listFactoryOrders(customerOrderId, null));
        result.put("priceAdjustments", jdbcTemplate.queryForList(
                "SELECT * FROM customer_order_price_adjustment WHERE customer_order_id=? ORDER BY id DESC", customerOrderId));
        result.put("confirmations", jdbcTemplate.queryForList(
                "SELECT * FROM customer_quote_confirmation WHERE customer_order_id=? ORDER BY id DESC", customerOrderId));
        result.put("paymentPlans", jdbcTemplate.queryForList(
                "SELECT * FROM customer_payment_plan WHERE customer_order_id=? ORDER BY plan_version DESC,installment_no", customerOrderId));
        result.put("receipts", jdbcTemplate.queryForList(
                "SELECT * FROM customer_payment_receipt WHERE customer_order_id=? ORDER BY id DESC", customerOrderId));
        result.put("releaseRequests", jdbcTemplate.queryForList(
                "SELECT * FROM cutting_release_request WHERE customer_order_id=? ORDER BY id DESC", customerOrderId));
        return result;
    }

    public Map<String, Object> workQueues() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("priceAdjustments", jdbcTemplate.queryForList("""
                SELECT pa.*,co.customer_order_no,c.customer_name FROM customer_order_price_adjustment pa
                JOIN customer_order co ON co.id=pa.customer_order_id JOIN customer c ON c.id=co.customer_id
                WHERE pa.status='待厂长审批' ORDER BY pa.requested_at
                """));
        result.put("paymentPlans", jdbcTemplate.queryForList("""
                SELECT pp.customer_order_id,pp.plan_version,MIN(pp.due_date) first_due_date,SUM(pp.planned_amount) total_amount,
                       co.customer_order_no,c.customer_name FROM customer_payment_plan pp
                JOIN customer_order co ON co.id=pp.customer_order_id JOIN customer c ON c.id=co.customer_id
                WHERE pp.status='待厂长审批' GROUP BY pp.customer_order_id,pp.plan_version,co.customer_order_no,c.customer_name
                ORDER BY first_due_date
                """));
        result.put("receipts", jdbcTemplate.queryForList("""
                SELECT r.*,co.customer_order_no,c.customer_name FROM customer_payment_receipt r
                JOIN customer_order co ON co.id=r.customer_order_id JOIN customer c ON c.id=co.customer_id
                WHERE r.status='待财务确认' ORDER BY r.created_at
                """));
        result.put("financeReleases", jdbcTemplate.queryForList("""
                SELECT cr.*,co.customer_order_no,c.customer_name FROM cutting_release_request cr
                JOIN customer_order co ON co.id=cr.customer_order_id JOIN customer c ON c.id=co.customer_id
                WHERE cr.status='待财务确认' ORDER BY cr.requested_at
                """));
        result.put("directorReleases", jdbcTemplate.queryForList("""
                SELECT cr.*,co.customer_order_no,c.customer_name FROM cutting_release_request cr
                JOIN customer_order co ON co.id=cr.customer_order_id JOIN customer c ON c.id=co.customer_id
                WHERE cr.status='待厂长审批' ORDER BY cr.requested_at
                """));
        result.put("serviceReleases", jdbcTemplate.queryForList("""
                SELECT cr.*,co.customer_order_no,c.customer_name FROM cutting_release_request cr
                JOIN customer_order co ON co.id=cr.customer_order_id JOIN customer c ON c.id=co.customer_id
                WHERE cr.status='可放行' ORDER BY cr.requested_at
                """));
        result.put("paymentReminders", listPaymentReminders());
        return result;
    }

    @Transactional
    public Map<String, Object> submitReceipt(Long customerOrderId, OrderFlowController.ReceiptRequest req) {
        requireRole("SERVICE", "FINANCE", "ADMIN");
        customerOrder(customerOrderId);
        String accountSnapshot = null;
        if (req.receivingAccountId() != null) {
            Map<String, Object> account = jdbcTemplate.queryForMap("SELECT * FROM receiving_account WHERE id=?", req.receivingAccountId());
            accountSnapshot = account.get("account_name") + " / " + account.get("bank_name") + " / " + account.get("account_no_masked");
        }
        KeyHolder key = new GeneratedKeyHolder();
        String snapshot = accountSnapshot;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO customer_payment_receipt(customer_order_id,payment_plan_id,receiving_account_id,actual_amount,
                    received_at,payment_method,account_snapshot,payer_name,bank_reference,voucher_path,status,submitted_by,created_at)
                    VALUES(?,?,?,?,?,?,?,?,?,?,'待财务确认',?,NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerOrderId); ps.setObject(2, req.paymentPlanId()); ps.setObject(3, req.receivingAccountId());
            ps.setBigDecimal(4, positive(req.actualAmount(), "到账金额必须大于0"));
            ps.setObject(5, req.receivedAt() == null ? LocalDateTime.now() : req.receivedAt());
            ps.setString(6, required(req.paymentMethod(), "付款方式不能为空")); ps.setString(7, snapshot);
            ps.setString(8, trim(req.payerName())); ps.setString(9, trim(req.bankReference()));
            ps.setString(10, trim(req.voucherPath())); ps.setLong(11, userId()); return ps;
        }, key);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_payment_receipt WHERE id=?", key.getKey().longValue());
    }

    @Transactional
    public Map<String, Object> confirmReceipt(Long receiptId, OrderFlowController.ApprovalRequest req) {
        requireRole("FINANCE", "ADMIN");
        Map<String, Object> receipt = jdbcTemplate.queryForMap("SELECT * FROM customer_payment_receipt WHERE id=?", receiptId);
        String status = Boolean.TRUE.equals(req.approved()) ? "财务已确认" : "财务已驳回";
        jdbcTemplate.update("""
                UPDATE customer_payment_receipt SET status=?,confirmed_by=?,confirmed_at=NOW(),finance_remark=? WHERE id=?
                """, status, userId(), trim(req.remark()), receiptId);
        if (Boolean.TRUE.equals(req.approved())) refreshPaymentStatus(longValue(receipt.get("customer_order_id")));
        refreshMonthlyOverdueLocks();
        return jdbcTemplate.queryForMap("SELECT * FROM customer_payment_receipt WHERE id=?", receiptId);
    }

    @Transactional
    public Map<String, Object> requestCuttingRelease(Long customerOrderId, OrderFlowController.CuttingReleaseRequest req) {
        requireRole("SERVICE", "ADMIN");
        Map<String, Object> order = customerOrder(customerOrderId);
        String settlement = String.valueOf(order.get("settlement_type"));
        String paymentStatus = String.valueOf(order.get("payment_status"));
        Long customerId = longValue(order.get("customer_id"));
        boolean monthlyOverdue = isMonthlyOverdue(customerId);
        boolean requiresDirector = !("FULL".equalsIgnoreCase(settlement) && "财务确认全款".equals(paymentStatus))
                && !("MONTHLY".equalsIgnoreCase(settlement) && !monthlyOverdue);
        if (requiresDirector && !Boolean.TRUE.equals(req.bossVerbalApproved())) {
            throw new IllegalArgumentException("该结算情况需要记录老板已口头同意");
        }
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO cutting_release_request(customer_order_id,request_type,requires_director_approval,
                    boss_verbal_approved,boss_confirmed_at,request_reason,status,requested_by,requested_at)
                    VALUES(?,?,?,?,?,?,'待财务确认',?,NOW())
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerOrderId); ps.setString(2, required(req.requestType(), "申请类型不能为空"));
            ps.setInt(3, requiresDirector ? 1 : 0); ps.setInt(4, Boolean.TRUE.equals(req.bossVerbalApproved()) ? 1 : 0);
            ps.setObject(5, req.bossConfirmedAt()); ps.setString(6, trim(req.requestReason())); ps.setLong(7, userId());
            return ps;
        }, key);
        jdbcTemplate.update("UPDATE customer_order SET cutting_release_status='待财务确认',updated_at=NOW() WHERE id=?", customerOrderId);
        return releaseRequest(key.getKey().longValue());
    }

    @Transactional
    public Map<String, Object> financeConfirmRelease(Long requestId, OrderFlowController.ApprovalRequest req) {
        requireRole("FINANCE", "ADMIN");
        Map<String, Object> row = releaseRequest(requestId);
        boolean approved = Boolean.TRUE.equals(req.approved());
        boolean needsDirector = intValue(row.get("requires_director_approval")) == 1;
        String status = approved ? (needsDirector ? "待厂长审批" : "可放行") : "财务已驳回";
        jdbcTemplate.update("""
                UPDATE cutting_release_request SET status=?,finance_confirmed_by=?,finance_confirmed_at=NOW(),finance_remark=? WHERE id=?
                """, status, userId(), trim(req.remark()), requestId);
        jdbcTemplate.update("UPDATE customer_order SET cutting_release_status=?,updated_at=NOW() WHERE id=?", status, row.get("customer_order_id"));
        return releaseRequest(requestId);
    }

    @Transactional
    public Map<String, Object> directorApproveRelease(Long requestId, OrderFlowController.ApprovalRequest req) {
        requireDirector();
        Map<String, Object> row = releaseRequest(requestId);
        if (!"待厂长审批".equals(String.valueOf(row.get("status")))) throw new IllegalArgumentException("当前申请不在厂长审批节点");
        String status = Boolean.TRUE.equals(req.approved()) ? "可放行" : "厂长已驳回";
        jdbcTemplate.update("""
                UPDATE cutting_release_request SET status=?,director_approved_by=?,director_approved_at=NOW(),director_remark=? WHERE id=?
                """, status, userId(), trim(req.remark()), requestId);
        jdbcTemplate.update("UPDATE customer_order SET cutting_release_status=?,updated_at=NOW() WHERE id=?", status, row.get("customer_order_id"));
        return releaseRequest(requestId);
    }

    @Transactional
    public Map<String, Object> releaseToCutting(Long requestId) {
        requireRole("SERVICE", "ADMIN");
        Map<String, Object> row = releaseRequest(requestId);
        if (!"可放行".equals(String.valueOf(row.get("status")))) throw new IllegalArgumentException("财务或厂长尚未完成放行确认");
        jdbcTemplate.update("UPDATE cutting_release_request SET status='已放行',released_by=?,released_at=NOW() WHERE id=?", userId(), requestId);
        jdbcTemplate.update("UPDATE customer_order SET cutting_release_status='已放行',updated_at=NOW() WHERE id=?", row.get("customer_order_id"));
        jdbcTemplate.update("UPDATE factory_order SET status='待下料',updated_at=NOW() WHERE customer_order_id=?", row.get("customer_order_id"));
        return releaseRequest(requestId);
    }

    private void refreshCustomerOrderQuote(Long customerOrderId) {
        BigDecimal total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(discounted_quote_amount),0) FROM factory_order WHERE customer_order_id=?
                """, BigDecimal.class, customerOrderId);
        jdbcTemplate.update("""
                UPDATE customer_order SET quote_total_amount=?,price_adjustment_amount=0,final_receivable_amount=?,
                quote_status='报价中',updated_at=NOW() WHERE id=?
                """, total, total, customerOrderId);
        refreshPaymentStatus(customerOrderId);
    }

    private void refreshPaymentStatus(Long customerOrderId) {
        Map<String, Object> order = customerOrder(customerOrderId);
        BigDecimal received = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(actual_amount),0) FROM customer_payment_receipt
                WHERE customer_order_id=? AND status='财务已确认'
                """, BigDecimal.class, customerOrderId);
        BigDecimal due = decimal(order.get("final_receivable_amount"));
        String status = received.compareTo(due) >= 0 ? "财务确认全款" : (received.signum() > 0 ? "财务确认部分到账" : "未收款");
        jdbcTemplate.update("UPDATE customer_order SET payment_status=?,updated_at=NOW() WHERE id=?", status, customerOrderId);
    }

    private boolean isMonthlyOverdue(Long customerId) {
        refreshMonthlyOverdueLocks();
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM customer_monthly_account WHERE customer_id=? AND enabled=1 AND overdue_locked=1
                """, Integer.class, customerId);
        return count != null && count > 0;
    }

    private void refreshMonthlyOverdueLocks() {
        List<Map<String, Object>> accounts = jdbcTemplate.queryForList("SELECT customer_id FROM customer_monthly_account WHERE enabled=1");
        for (Map<String, Object> account : accounts) {
            Long customerId = longValue(account.get("customer_id"));
            Integer overdue = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM customer_order co
                    JOIN customer_payment_plan pp ON pp.customer_order_id=co.id AND pp.status='已批准'
                    WHERE co.customer_id=? AND co.settlement_type='MONTHLY' AND pp.due_date<CURDATE()
                      AND pp.planned_amount>(SELECT COALESCE(SUM(r.actual_amount),0) FROM customer_payment_receipt r
                        WHERE r.payment_plan_id=pp.id AND r.status='财务已确认')
                    """, Integer.class, customerId);
            boolean locked = overdue != null && overdue > 0;
            jdbcTemplate.update("""
                    UPDATE customer_monthly_account
                    SET overdue_locked=?,locked_at=CASE WHEN ?=1 AND overdue_locked=0 THEN NOW() ELSE locked_at END,
                        unlocked_at=CASE WHEN ?=0 AND overdue_locked=1 THEN NOW() ELSE unlocked_at END,updated_at=NOW()
                    WHERE customer_id=?
                    """, locked ? 1 : 0, locked ? 1 : 0, locked ? 1 : 0, customerId);
        }
    }

    private Map<String, Object> customerOrder(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM customer_order WHERE id=? AND is_deleted=0", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("客户订单不存在");
        return rows.getFirst();
    }

    private Map<String, Object> factoryOrder(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM factory_order WHERE factory_order_id=?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("工厂订单不存在");
        return rows.getFirst();
    }

    private Map<String, Object> quoteWithItems(long quoteId) {
        Map<String, Object> quote = jdbcTemplate.queryForMap("SELECT * FROM factory_order_quote WHERE id=?", quoteId);
        List<Map<String, Object>> items = jdbcTemplate.queryForList("SELECT * FROM factory_order_quote_item WHERE quote_id=? ORDER BY sort_order,id", quoteId);
        for (Map<String, Object> item : items) {
            item.put("extra_prices", jdbcTemplate.queryForList(
                    "SELECT * FROM factory_order_quote_item_extra_price WHERE quote_item_id=? ORDER BY id", item.get("id")));
        }
        quote.put("items", items);
        return quote;
    }

    private Map<String, Object> releaseRequest(Long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM cutting_release_request WHERE id=?", id);
    }

    private synchronized String generateFactoryOrderId(String lineCode, boolean supplement) {
        String prefix = (supplement ? "BD" : "") + normalizeLineCode(lineCode)
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Integer seq = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(RIGHT(factory_order_id,3) AS UNSIGNED)),0)+1
                FROM factory_order WHERE factory_order_id LIKE ?
                """, Integer.class, prefix + "%");
        if (seq == null || seq > 999) throw new IllegalArgumentException("当日该生产线订单序号已用完");
        return prefix + String.format("%03d", seq);
    }

    private void requireExists(String table, String column, Object value) {
        if (value == null) throw new IllegalArgumentException(column + "不能为空");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM " + table + " WHERE " + column + "=?", Integer.class, value);
        if (count == null || count == 0) throw new IllegalArgumentException(table + "记录不存在");
    }

    private Long userId() {
        CurrentUser user = AuthContext.get();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("当前用户未登录");
        return user.getId();
    }

    private String role() {
        CurrentUser user = AuthContext.get();
        return user == null || user.getRoleCode() == null ? "" : user.getRoleCode().toUpperCase(Locale.ROOT);
    }

    private boolean isAdmin() { return "ADMIN".equals(role()); }
    private void requireAdmin() { requireRole("ADMIN"); }
    private void requireDirector() { requireRole("DIRECTOR", "ADMIN"); }

    private void requireRole(String... allowed) {
        String actual = role();
        for (String value : allowed) if (value.equals(actual)) return;
        throw new IllegalArgumentException("当前角色没有执行该操作的权限");
    }

    private String normalizeLineCode(String value) {
        String code = required(value, "生产线编码不能为空").toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9]{1,10}")) throw new IllegalArgumentException("生产线编码只能包含英文和数字");
        return code;
    }

    private String normalizeOrderType(String value) {
        String type = value == null ? "NORMAL" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("NORMAL", "SUPPLEMENT").contains(type)) throw new IllegalArgumentException("订单类型无效");
        return type;
    }

    private boolean isHardware(String category) {
        String value = trim(category);
        return "HARDWARE".equalsIgnoreCase(value) || "五金配件".equals(value);
    }

    private QuoteCalcResult calculateHardware(QuoteCalcRequest item) {
        BigDecimal quantity = new BigDecimal(item.getQuantity());
        BigDecimal amount = item.getBaseUnitPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        QuoteCalcResult result = new QuoteCalcResult();
        result.setAreaM2(quantity);
        result.setSpecialAdjustTotal(BigDecimal.ZERO);
        result.setFinalUnitPrice(item.getBaseUnitPrice());
        result.setAmount(amount);
        result.setAppliedRules(List.of());
        return result;
    }

    private boolean isRuleDiscountEligible(QuoteCalcRequest item, Map<String, Object> rule) {
        if (isHardware(item.getProductCategory())) return false;
        Long sourceId = nullableLong(rule.get("source_rule_id"));
        if (sourceId != null && item.getNonDiscountRuleIds() != null && item.getNonDiscountRuleIds().contains(sourceId)) return false;
        String name = String.valueOf(rule.getOrDefault("rule_name", ""));
        return item.getNonDiscountCustomRuleNames() == null || !item.getNonDiscountCustomRuleNames().contains(name);
    }

    private Long nullableLong(Object value) {
        if (value == null) return null;
        long result = Long.parseLong(String.valueOf(value));
        return result == 0 ? null : result;
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("报价规则数据格式错误");
        }
    }

    private String required(String value, String message) {
        String result = trim(value);
        if (result == null) throw new IllegalArgumentException(message);
        return result;
    }

    private String trim(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private BigDecimal positive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(message);
        return value;
    }

    private BigDecimal nonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(message);
        return value;
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    private Long longValue(Object value) { return value == null ? null : Long.valueOf(String.valueOf(value)); }
    private int intValue(Object value) { return value == null ? 0 : Integer.parseInt(String.valueOf(value)); }
    private int nvl(Integer value, int fallback) { return value == null ? fallback : value; }
}

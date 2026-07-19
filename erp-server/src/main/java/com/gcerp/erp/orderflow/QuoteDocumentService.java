package com.gcerp.erp.orderflow;

import com.gcerp.erp.auth.AuthContext;
import com.gcerp.erp.auth.CurrentUser;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteDocumentService {
    private static final Path PDF_ROOT = Path.of("upload", "customer-quote-pdf");
    private static final Path ATTACHMENT_ROOT = Path.of("upload", "quote-attachment");
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> uploadAttachment(MultipartFile file) throws IOException {
        requireQuoteRole();
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("报价图片不能为空");
        Files.createDirectories(ATTACHMENT_ROOT);
        String original = Path.of(file.getOriginalFilename() == null ? "quote-image" : file.getOriginalFilename()).getFileName().toString();
        String ext = extension(original);
        String stored = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_"
                + UUID.randomUUID().toString().replace("-", "") + (ext == null ? "" : "." + ext);
        Path target = ATTACHMENT_ROOT.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return Map.of("fileName", original, "filePath", target.toString().replace("\\", "/"));
    }

    public Map<String, Object> companyProfile() {
        return jdbcTemplate.queryForMap("SELECT * FROM company_profile WHERE id=1");
    }

    @Transactional
    public Map<String, Object> updateCompanyProfile(QuoteDocumentController.CompanyProfileRequest req) {
        requireAdmin();
        jdbcTemplate.update("""
                UPDATE company_profile SET company_name=?,logo_path=?,company_address=?,contact_phone=?,updated_at=NOW() WHERE id=1
                """, required(req.companyName(), "公司名称不能为空"), trim(req.logoPath()), trim(req.companyAddress()), trim(req.contactPhone()));
        return companyProfile();
    }

    @Transactional
    public Map<String, Object> generatePdf(Long customerOrderId, QuoteDocumentController.QuotePdfRequest req) throws Exception {
        requireService();
        Map<String, Object> order = orderSummary(customerOrderId);
        List<Map<String, Object>> factories = currentFactoryQuotes(customerOrderId);
        if (factories.isEmpty()) throw new IllegalArgumentException("客户订单没有已提交的工厂订单报价");
        Integer incomplete = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM factory_order fo
                JOIN production_line pl ON pl.id=fo.production_line_id
                LEFT JOIN factory_order_external_quote eq
                  ON eq.factory_order_id=fo.factory_order_id AND eq.status='有效'
                WHERE fo.customer_order_id=? AND (
                  (pl.quote_mode='ERP' AND fo.current_quote_version=0) OR
                  (pl.quote_mode='EXTERNAL' AND (eq.id IS NULL OR eq.confirmation_status<>'客户已确认'))
                )
                """, Integer.class, customerOrderId);
        if (incomplete != null && incomplete > 0) throw new IllegalArgumentException("仍有L线报价未提交或V线外部报价未确认");
        Integer pendingAdjustment = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM customer_order_price_adjustment WHERE customer_order_id=? AND status='待厂长审批'
                """, Integer.class, customerOrderId);
        if (pendingAdjustment != null && pendingAdjustment > 0) throw new IllegalArgumentException("成交价调整仍在等待厂长审批");

        BigDecimal rate = req.taxRate() == null ? decimal(order.get("tax_rate")) : req.taxRate();
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("税率必须在0到1之间");
        int validDays = req.validDays() == null ? 15 : req.validDays();
        if (validDays <= 0 || validDays > 365) throw new IllegalArgumentException("报价有效期必须在1到365天之间");

        Map<String, BigDecimal> amounts = calculateSummary(customerOrderId, order, rate);
        Integer nextVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(pdf_version),0)+1 FROM customer_quote_pdf WHERE customer_order_id=?
                """, Integer.class, customerOrderId);
        int version = nextVersion == null ? 1 : nextVersion;
        String orderNo = safe(String.valueOf(order.get("customer_order_no")));
        Path dir = PDF_ROOT.resolve(orderNo);
        Files.createDirectories(dir);
        Path target = dir.resolve("quote-v" + version + ".pdf");
        writePdf(target, order, factories, customerOrderId, amounts, rate, validDays, trim(req.quoteRemark()));

        jdbcTemplate.update("UPDATE customer_quote_confirmation SET status='已失效' WHERE customer_order_id=? AND status='有效'", customerOrderId);
        jdbcTemplate.update("UPDATE customer_quote_pdf SET status='已失效',invalidated_at=NOW() WHERE customer_order_id=? AND status<>'已失效'", customerOrderId);
        jdbcTemplate.update("""
                INSERT INTO customer_quote_pdf(customer_order_id,pdf_version,status,tax_rate,valid_days,
                product_craft_original,product_craft_discount,non_discount_craft,hardware_amount,external_quote_amount,price_adjustment,
                untaxed_total,tax_amount,tax_included_total,quote_remark,file_path,generated_by,generated_at)
                VALUES(?,?,'待客户确认',?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
                """, customerOrderId, version, rate, validDays, amounts.get("productOriginal"), amounts.get("productDiscount"),
                amounts.get("nonDiscountCraft"), amounts.get("hardware"), amounts.get("external"), amounts.get("adjustment"), amounts.get("untaxed"),
                amounts.get("tax"), amounts.get("taxIncluded"), trim(req.quoteRemark()), target.toString().replace("\\", "/"), userId());
        jdbcTemplate.update("""
                UPDATE customer_order SET tax_rate=?,quote_valid_days=?,final_receivable_amount=?,updated_at=NOW() WHERE id=?
                """, rate, validDays, amounts.get("taxIncluded"), customerOrderId);
        return jdbcTemplate.queryForMap("SELECT * FROM customer_quote_pdf WHERE customer_order_id=? AND pdf_version=?", customerOrderId, version);
    }

    public List<Map<String, Object>> listPdfs(Long customerOrderId) {
        return jdbcTemplate.queryForList("""
                SELECT p.*,u.display_name AS generated_by_name FROM customer_quote_pdf p
                LEFT JOIN app_user u ON u.id=p.generated_by WHERE p.customer_order_id=? ORDER BY p.pdf_version DESC
                """, customerOrderId);
    }

    public Map<String, Object> pdf(Long id) {
        return jdbcTemplate.queryForMap("SELECT * FROM customer_quote_pdf WHERE id=?", id);
    }

    private Map<String, Object> orderSummary(Long id) {
        return jdbcTemplate.queryForMap("""
                SELECT co.*,c.customer_name,c.phone AS customer_phone,c.address AS customer_address,
                       s.display_name AS service_staff_name
                FROM customer_order co JOIN customer c ON c.id=co.customer_id
                LEFT JOIN app_user s ON s.id=co.service_staff_id WHERE co.id=? AND co.is_deleted=0
                """, id);
    }

    private List<Map<String, Object>> currentFactoryQuotes(Long customerOrderId) {
        return jdbcTemplate.queryForList("""
                SELECT fo.factory_order_id,fo.factory_order_name,fo.discount_rate,fo.original_quote_amount,
                       fo.discounted_quote_amount,q.id AS quote_id,q.version_no,u.display_name AS quote_assignee_name
                FROM factory_order fo
                JOIN production_line pl ON pl.id=fo.production_line_id AND pl.quote_mode='ERP'
                JOIN factory_order_quote q ON q.factory_order_id=fo.factory_order_id AND q.version_no=fo.current_quote_version
                LEFT JOIN app_user u ON u.id=fo.quote_assignee_id
                WHERE fo.customer_order_id=? ORDER BY fo.split_sequence
                """, customerOrderId);
    }

    private Map<String, BigDecimal> calculateSummary(Long customerOrderId, Map<String, Object> order, BigDecimal taxRate) {
        BigDecimal productOriginal = scalar("""
                SELECT COALESCE(SUM(i.original_amount),0) FROM factory_order_quote_item i
                JOIN factory_order_quote q ON q.id=i.quote_id JOIN factory_order fo ON fo.factory_order_id=q.factory_order_id
                WHERE fo.customer_order_id=? AND q.version_no=fo.current_quote_version AND i.product_category<>'HARDWARE'
                """, customerOrderId);
        BigDecimal productFinal = scalar("""
                SELECT COALESCE(SUM(i.final_amount),0) FROM factory_order_quote_item i
                JOIN factory_order_quote q ON q.id=i.quote_id JOIN factory_order fo ON fo.factory_order_id=q.factory_order_id
                WHERE fo.customer_order_id=? AND q.version_no=fo.current_quote_version AND i.product_category<>'HARDWARE'
                """, customerOrderId);
        BigDecimal hardware = scalar("""
                SELECT COALESCE(SUM(i.final_amount),0) FROM factory_order_quote_item i
                JOIN factory_order_quote q ON q.id=i.quote_id JOIN factory_order fo ON fo.factory_order_id=q.factory_order_id
                WHERE fo.customer_order_id=? AND q.version_no=fo.current_quote_version AND i.product_category='HARDWARE'
                """, customerOrderId);
        BigDecimal nonDiscountCraft = scalar("""
                SELECT COALESCE(SUM(e.final_charge),0) FROM factory_order_quote_item_extra_price e
                JOIN factory_order_quote q ON q.id=e.quote_id JOIN factory_order fo ON fo.factory_order_id=q.factory_order_id
                JOIN factory_order_quote_item i ON i.id=e.quote_item_id
                WHERE fo.customer_order_id=? AND q.version_no=fo.current_quote_version
                  AND i.product_category<>'HARDWARE' AND e.discount_eligible=0
                """, customerOrderId);
        BigDecimal external = scalar("""
                SELECT COALESCE(SUM(eq.final_amount),0) FROM factory_order_external_quote eq
                JOIN factory_order fo ON fo.factory_order_id=eq.factory_order_id
                JOIN production_line pl ON pl.id=fo.production_line_id AND pl.quote_mode='EXTERNAL'
                WHERE fo.customer_order_id=? AND eq.status='有效' AND eq.version_no=fo.current_quote_version
                """, customerOrderId);
        BigDecimal adjustment = decimal(order.get("price_adjustment_amount"));
        BigDecimal untaxed = productFinal.add(hardware).add(external).add(adjustment).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = untaxed.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        return Map.of(
                "productOriginal", productOriginal,
                "productDiscount", productOriginal.subtract(productFinal),
                "nonDiscountCraft", nonDiscountCraft,
                "hardware", hardware,
                "external", external,
                "adjustment", adjustment,
                "untaxed", untaxed,
                "tax", tax,
                "taxIncluded", untaxed.add(tax)
        );
    }

    private void writePdf(Path target, Map<String, Object> order, List<Map<String, Object>> factories,
                          Long customerOrderId, Map<String, BigDecimal> amounts, BigDecimal taxRate,
                          int validDays, String remark) throws Exception {
        Document document = new Document(PageSize.A4, 28, 28, 30, 30);
        PdfWriter.getInstance(document, new FileOutputStream(target.toFile()));
        document.open();
        Font title = font(18, Font.BOLD);
        Font head = font(10, Font.BOLD);
        Font normal = font(9, Font.NORMAL);
        Map<String, Object> company = companyProfile();
        Object logoPath = company.get("logo_path");
        if (logoPath != null && Files.exists(Path.of(String.valueOf(logoPath)))) {
            Image logo = Image.getInstance(String.valueOf(logoPath));
            logo.scaleToFit(130, 42); logo.setAlignment(Element.ALIGN_CENTER); document.add(logo);
        }
        Paragraph companyName = new Paragraph(String.valueOf(company.get("company_name")), title);
        companyName.setAlignment(Element.ALIGN_CENTER);
        document.add(companyName);
        Paragraph quoteTitle = new Paragraph("客户订单报价单", font(14, Font.BOLD));
        quoteTitle.setAlignment(Element.ALIGN_CENTER);
        quoteTitle.setSpacingAfter(10);
        document.add(quoteTitle);
        PdfPTable info = new PdfPTable(new float[]{1, 2, 1, 2}); info.setWidthPercentage(100);
        addPair(info, "客户名称", order.get("customer_name"), head, normal);
        addPair(info, "客户订单号", order.get("customer_order_no"), head, normal);
        addPair(info, "客户联系人", value(order.get("customer_name")), head, normal);
        addPair(info, "报价日期", LocalDate.now(), head, normal);
        addPair(info, "有效期", validDays + "天", head, normal);
        addPair(info, "客服", value(order.get("service_staff_name")), head, normal);
        addPair(info, "客户电话", value(order.get("customer_phone")), head, normal);
        addPair(info, "公司电话", value(company.get("contact_phone")), head, normal);
        addPair(info, "报价状态", "待客户确认", head, normal);
        addPair(info, "公司地址", value(company.get("company_address")), head, normal);
        addPair(info, "税率", percent(taxRate), head, normal);
        document.add(info);

        for (Map<String, Object> factory : factories) {
            Paragraph factoryTitle = new Paragraph(factory.get("factory_order_id") + " / " + factory.get("factory_order_name")
                    + "    报价负责人：" + value(factory.get("quote_assignee_name")), head);
            factoryTitle.setSpacingBefore(12); factoryTitle.setSpacingAfter(5); document.add(factoryTitle);
            PdfPTable table = new PdfPTable(new float[]{0.7f, 2.1f, 1.5f, 1.5f, 0.7f, 0.8f, 1.1f, 1.1f, 1.1f});
            table.setWidthPercentage(100);
            for (String h : List.of("图片","产品名称","材质/规格","尺寸","数量","单位","单价","原金额","折后金额")) addCell(table, h, head, true);
            List<Map<String, Object>> items = jdbcTemplate.queryForList("SELECT * FROM factory_order_quote_item WHERE quote_id=? ORDER BY sort_order,id", factory.get("quote_id"));
            for (Map<String, Object> item : items) {
                addImageCell(table, item.get("attachment_path"));
                addCell(table, item.get("product_name"), normal, false);
                addCell(table, value(item.get("material_structure")) + " " + value(item.get("specification")), normal, false);
                addCell(table, dimensions(item), normal, false);
                addCell(table, item.get("quantity"), normal, false); addCell(table, item.get("unit"), normal, false);
                addCell(table, money(item.get("base_unit_price")), normal, false);
                addCell(table, money(item.get("original_amount")), normal, false);
                addCell(table, money(item.get("final_amount")), normal, false);
            }
            document.add(table);
        }

        PdfPTable summary = new PdfPTable(new float[]{3, 1.4f}); summary.setWidthPercentage(55); summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.setSpacingBefore(12);
        addSummary(summary, "产品及工艺原价", amounts.get("productOriginal"), head, normal);
        addSummary(summary, "产品及工艺折扣", amounts.get("productDiscount").negate(), head, normal);
        addSummary(summary, "产品及工艺折后金额", amounts.get("productOriginal").subtract(amounts.get("productDiscount")), head, normal);
        addSummary(summary, "其中不参与折扣的特殊工艺", amounts.get("nonDiscountCraft"), head, normal);
        addSummary(summary, "五金配件（不参与折扣）", amounts.get("hardware"), head, normal);
        addSummary(summary, "板式外部报价汇总", amounts.get("external"), head, normal);
        addSummary(summary, "成交价调整", amounts.get("adjustment"), head, normal);
        addSummary(summary, "未税报价合计", amounts.get("untaxed"), head, normal);
        addSummary(summary, "税额（" + percent(taxRate) + "）", amounts.get("tax"), head, normal);
        addSummary(summary, "价税合计", amounts.get("taxIncluded"), font(10, Font.BOLD), font(10, Font.BOLD));
        document.add(summary);
        if (remark != null) { Paragraph p = new Paragraph("备注：" + remark, normal); p.setSpacingBefore(10); document.add(p); }
        document.close();
    }

    private Font font(float size, int style) throws Exception { return new Font(chineseFont(), size, style); }

    private BaseFont chineseFont() throws Exception {
        for (String candidate : List.of("C:/Windows/Fonts/msyh.ttc,0", "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0")) {
            String actual = candidate.replace(",0", "");
            if (Files.exists(Path.of(actual))) return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
        throw new IllegalStateException("未找到可用于PDF的中文字体");
    }

    private void addPair(PdfPTable table, String label, Object value, Font head, Font normal) {
        addCell(table, label, head, true); addCell(table, value, normal, false);
    }
    private void addSummary(PdfPTable table, String label, Object value, Font head, Font normal) {
        addCell(table, label, head, true); addCell(table, money(value), normal, false);
    }
    private void addCell(PdfPTable table, Object value, Font font, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(value(value), font)); cell.setPadding(5);
        if (header) cell.setBackgroundColor(new Color(238, 242, 245));
        table.addCell(cell);
    }
    private void addImageCell(PdfPTable table, Object pathValue) {
        try {
            if (pathValue != null && Files.exists(Path.of(String.valueOf(pathValue)))) {
                Image image = Image.getInstance(String.valueOf(pathValue)); image.scaleToFit(42, 36);
                PdfPCell cell = new PdfPCell(image, true); cell.setPadding(3); table.addCell(cell); return;
            }
        } catch (Exception ignored) { }
        addCell(table, "-", new Font(Font.HELVETICA, 9), false);
    }
    private String dimensions(Map<String, Object> item) {
        return value(item.get("width_mm")) + "×" + value(item.get("height_mm")) + "×" + value(item.get("thickness_mm"));
    }
    private BigDecimal scalar(String sql, Object... args) { return jdbcTemplate.queryForObject(sql, BigDecimal.class, args); }
    private BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value)); }
    private String money(Object value) { return decimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private String percent(BigDecimal value) { return value.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%"; }
    private String value(Object value) { return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value); }
    private String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String required(String value, String message) { String result = trim(value); if (result == null) throw new IllegalArgumentException(message); return result; }
    private String safe(String value) { return value.replaceAll("[^A-Za-z0-9_-]", "_"); }
    private String extension(String name) { int i = name.lastIndexOf('.'); return i < 0 ? null : name.substring(i + 1).toLowerCase(Locale.ROOT); }

    private Long userId() { CurrentUser user = AuthContext.get(); if (user == null) throw new IllegalArgumentException("当前用户未登录"); return user.getId(); }
    private String role() { CurrentUser user = AuthContext.get(); return user == null ? "" : user.getRoleCode().toUpperCase(Locale.ROOT); }
    private void requireAdmin() { if (!"ADMIN".equals(role())) throw new IllegalArgumentException("只有管理员可以维护公司资料"); }
    private void requireService() { if (!List.of("SERVICE","ADMIN").contains(role())) throw new IllegalArgumentException("只有客服可以生成客户报价PDF"); }
    private void requireQuoteRole() { if (!List.of("SERVICE","ENGINEER","ADMIN").contains(role())) throw new IllegalArgumentException("当前角色不能上传报价图片"); }
}

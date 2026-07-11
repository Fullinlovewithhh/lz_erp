package com.gcerp.erp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(20)
@RequiredArgsConstructor
public class V3SchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createCoreTables();
        addCustomerOrderColumns();
        createBusinessTables();
        addFullQuoteColumns();
    }

    private void createCoreTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS production_line (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  line_code VARCHAR(10) NOT NULL,
                  line_name VARCHAR(100) NOT NULL,
                  enabled TINYINT NOT NULL DEFAULT 1,
                  sort_order INT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  UNIQUE KEY uk_production_line_code (line_code)
                )
                """);
        jdbcTemplate.update("""
                INSERT IGNORE INTO production_line(line_code,line_name,enabled,sort_order,created_at,updated_at)
                VALUES ('L','L生产线',1,10,NOW(),NOW()),('V','V生产线',1,20,NOW(),NOW())
                """);
    }

    private void addCustomerOrderColumns() {
        jdbcTemplate.execute("ALTER TABLE customer_order MODIFY project_id BIGINT NULL");
        addColumn("customer_order", "service_staff_id", "BIGINT NULL");
        addColumn("customer_order", "review_engineer_id", "BIGINT NULL");
        addColumn("customer_order", "review_status", "VARCHAR(30) NOT NULL DEFAULT '待领取'");
        addColumn("customer_order", "split_instruction", "TEXT NULL");
        addColumn("customer_order", "settlement_type", "VARCHAR(30) NULL");
        addColumn("customer_order", "quote_status", "VARCHAR(30) NOT NULL DEFAULT '待拆单'");
        addColumn("customer_order", "quote_total_amount", "DECIMAL(14,2) NOT NULL DEFAULT 0");
        addColumn("customer_order", "price_adjustment_amount", "DECIMAL(14,2) NOT NULL DEFAULT 0");
        addColumn("customer_order", "final_receivable_amount", "DECIMAL(14,2) NOT NULL DEFAULT 0");
        addColumn("customer_order", "payment_status", "VARCHAR(30) NOT NULL DEFAULT '未收款'");
        addColumn("customer_order", "cutting_release_status", "VARCHAR(30) NOT NULL DEFAULT '未申请'");
        addColumn("customer_order", "tax_rate", "DECIMAL(6,4) NOT NULL DEFAULT 0");
        addColumn("customer_order", "quote_valid_days", "INT NOT NULL DEFAULT 15");
        addUniqueIndex("customer_order", "uk_customer_order_customer_no", "customer_id, customer_order_no");
    }

    private void createBusinessTables() {
        List<String> ddl = List.of(
                """
                CREATE TABLE IF NOT EXISTS customer_order_cad_file (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL,
                  file_name VARCHAR(255) NOT NULL, file_ext VARCHAR(20) NULL, file_size BIGINT NULL,
                  file_path VARCHAR(500) NOT NULL, created_by BIGINT NULL, created_at DATETIME NOT NULL,
                  KEY idx_customer_order_cad_order (customer_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_order_split_draft (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL,
                  factory_order_name VARCHAR(255) NOT NULL, production_line_id BIGINT NOT NULL,
                  order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL', parent_factory_order_id VARCHAR(50) NULL,
                  remark VARCHAR(500) NULL, sort_order INT NOT NULL DEFAULT 0, created_by BIGINT NOT NULL,
                  status VARCHAR(20) NOT NULL DEFAULT '草稿', created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
                  KEY idx_split_draft_order (customer_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS factory_order (
                  factory_order_id VARCHAR(50) PRIMARY KEY, customer_order_id BIGINT NOT NULL,
                  production_line_id BIGINT NOT NULL, factory_order_name VARCHAR(255) NOT NULL,
                  order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL', parent_factory_order_id VARCHAR(50) NULL,
                  split_sequence INT NOT NULL, quote_assignee_id BIGINT NULL, quote_assignee_role VARCHAR(30) NULL,
                  status VARCHAR(50) NOT NULL DEFAULT '待报价分配', discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1,
                  original_quote_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                  discounted_quote_amount DECIMAL(14,2) NOT NULL DEFAULT 0, current_quote_version INT NOT NULL DEFAULT 0,
                  supplement_credit_flag TINYINT NOT NULL DEFAULT 0, demand_desc TEXT NULL, created_by BIGINT NULL,
                  created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
                  UNIQUE KEY uk_factory_order_split_seq (customer_order_id, split_sequence),
                  KEY idx_factory_order_customer_order (customer_order_id), KEY idx_factory_order_assignee (quote_assignee_id),
                  KEY idx_factory_order_parent (parent_factory_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS factory_order_assignment_log (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, factory_order_id VARCHAR(50) NOT NULL,
                  from_user_id BIGINT NULL, to_user_id BIGINT NULL, action_type VARCHAR(30) NOT NULL,
                  reason VARCHAR(500) NULL, operated_by BIGINT NOT NULL, created_at DATETIME NOT NULL,
                  KEY idx_assignment_log_order (factory_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS factory_order_quote (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, factory_order_id VARCHAR(50) NOT NULL, version_no INT NOT NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '草稿', original_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                  discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1, discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                  final_amount DECIMAL(14,2) NOT NULL DEFAULT 0, quote_desc VARCHAR(500) NULL,
                  created_by BIGINT NOT NULL, created_at DATETIME NOT NULL, submitted_at DATETIME NULL,
                  UNIQUE KEY uk_factory_quote_version (factory_order_id, version_no)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS factory_order_quote_item (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, quote_id BIGINT NOT NULL, product_category VARCHAR(50) NOT NULL,
                  product_id BIGINT NULL, hardware_item_id BIGINT NULL, product_name VARCHAR(200) NOT NULL,
                  specification VARCHAR(500) NULL, quantity DECIMAL(12,4) NOT NULL, unit VARCHAR(30) NOT NULL,
                  unit_price DECIMAL(14,4) NOT NULL, original_amount DECIMAL(14,2) NOT NULL,
                  discount_eligible TINYINT NOT NULL DEFAULT 1, discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
                  final_amount DECIMAL(14,2) NOT NULL, remark VARCHAR(500) NULL, sort_order INT NOT NULL DEFAULT 0,
                  KEY idx_factory_quote_item_quote (quote_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_order_price_adjustment (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL, version_no INT NOT NULL,
                  before_amount DECIMAL(14,2) NOT NULL, after_amount DECIMAL(14,2) NOT NULL,
                  adjustment_amount DECIMAL(14,2) NOT NULL, reason VARCHAR(500) NOT NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '待厂长审批', requested_by BIGINT NOT NULL, requested_at DATETIME NOT NULL,
                  approved_by BIGINT NULL, approved_at DATETIME NULL, approval_remark VARCHAR(500) NULL,
                  KEY idx_price_adjustment_order (customer_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_quote_confirmation (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL, pdf_id BIGINT NULL,
                  confirmation_version INT NOT NULL,
                  confirmation_method VARCHAR(30) NOT NULL, confirmed_at DATETIME NOT NULL,
                  customer_contact VARCHAR(100) NOT NULL, confirmation_remark VARCHAR(500) NULL,
                  attachment_path VARCHAR(500) NULL, recorded_by BIGINT NOT NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '有效', created_at DATETIME NOT NULL,
                  KEY idx_quote_confirmation_order (customer_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS company_profile (
                  id BIGINT PRIMARY KEY, company_name VARCHAR(200) NOT NULL, logo_path VARCHAR(500) NULL,
                  company_address VARCHAR(500) NULL, contact_phone VARCHAR(100) NULL, updated_at DATETIME NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_quote_pdf (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL, pdf_version INT NOT NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '待客户确认', tax_rate DECIMAL(6,4) NOT NULL DEFAULT 0,
                  valid_days INT NOT NULL DEFAULT 15, product_craft_original DECIMAL(14,2) NOT NULL,
                  product_craft_discount DECIMAL(14,2) NOT NULL, non_discount_craft DECIMAL(14,2) NOT NULL,
                  hardware_amount DECIMAL(14,2) NOT NULL, price_adjustment DECIMAL(14,2) NOT NULL,
                  untaxed_total DECIMAL(14,2) NOT NULL, tax_amount DECIMAL(14,2) NOT NULL,
                  tax_included_total DECIMAL(14,2) NOT NULL, quote_remark VARCHAR(1000) NULL,
                  file_path VARCHAR(500) NOT NULL, generated_by BIGINT NOT NULL, generated_at DATETIME NOT NULL,
                  invalidated_at DATETIME NULL, UNIQUE KEY uk_customer_quote_pdf_version (customer_order_id,pdf_version),
                  KEY idx_customer_quote_pdf_order (customer_order_id)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS receiving_account (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, account_code VARCHAR(50) NOT NULL, account_name VARCHAR(100) NOT NULL,
                  payment_method VARCHAR(30) NOT NULL, bank_name VARCHAR(100) NULL, account_no_masked VARCHAR(100) NULL,
                  enabled TINYINT NOT NULL DEFAULT 1, sort_order INT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
                  UNIQUE KEY uk_receiving_account_code (account_code)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_payment_plan (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL, plan_version INT NOT NULL,
                  installment_no INT NOT NULL, planned_amount DECIMAL(14,2) NOT NULL, due_date DATE NOT NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '待厂长审批', customer_confirmed TINYINT NOT NULL DEFAULT 0,
                  requested_by BIGINT NOT NULL, approved_by BIGINT NULL, approved_at DATETIME NULL,
                  approval_remark VARCHAR(500) NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
                  UNIQUE KEY uk_payment_plan_installment (customer_order_id, plan_version, installment_no),
                  KEY idx_payment_plan_due (due_date, status)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_payment_receipt (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL, payment_plan_id BIGINT NULL,
                  receiving_account_id BIGINT NULL, actual_amount DECIMAL(14,2) NOT NULL, received_at DATETIME NOT NULL,
                  payment_method VARCHAR(30) NOT NULL, account_snapshot VARCHAR(255) NULL, payer_name VARCHAR(100) NULL,
                  bank_reference VARCHAR(255) NULL, voucher_path VARCHAR(500) NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '待财务确认', submitted_by BIGINT NOT NULL,
                  confirmed_by BIGINT NULL, confirmed_at DATETIME NULL, finance_remark VARCHAR(500) NULL,
                  created_at DATETIME NOT NULL, KEY idx_payment_receipt_order (customer_order_id),
                  KEY idx_payment_receipt_status (status)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customer_monthly_account (
                  customer_id BIGINT PRIMARY KEY, enabled TINYINT NOT NULL DEFAULT 0,
                  overdue_locked TINYINT NOT NULL DEFAULT 0, locked_reason VARCHAR(500) NULL,
                  locked_at DATETIME NULL, unlocked_at DATETIME NULL, updated_at DATETIME NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS cutting_release_request (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL, request_type VARCHAR(30) NOT NULL,
                  requires_director_approval TINYINT NOT NULL, boss_verbal_approved TINYINT NOT NULL DEFAULT 0,
                  boss_confirmed_at DATETIME NULL, request_reason VARCHAR(500) NULL,
                  status VARCHAR(30) NOT NULL DEFAULT '待财务确认', requested_by BIGINT NOT NULL, requested_at DATETIME NOT NULL,
                  finance_confirmed_by BIGINT NULL, finance_confirmed_at DATETIME NULL, finance_remark VARCHAR(500) NULL,
                  director_approved_by BIGINT NULL, director_approved_at DATETIME NULL, director_remark VARCHAR(500) NULL,
                  released_by BIGINT NULL, released_at DATETIME NULL, KEY idx_cutting_release_order (customer_order_id),
                  KEY idx_cutting_release_status (status)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS hardware_item (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, hardware_code VARCHAR(50) NOT NULL,
                  hardware_name VARCHAR(150) NOT NULL, specification VARCHAR(255) NULL, unit VARCHAR(30) NOT NULL,
                  sale_price DECIMAL(14,4) NOT NULL DEFAULT 0, stock_mode VARCHAR(20) NOT NULL DEFAULT 'STOCK',
                  enabled TINYINT NOT NULL DEFAULT 1, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
                  UNIQUE KEY uk_hardware_code (hardware_code)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS hardware_stock (
                  hardware_item_id BIGINT PRIMARY KEY, on_hand_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
                  reserved_quantity DECIMAL(14,4) NOT NULL DEFAULT 0, available_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
                  min_stock_quantity DECIMAL(14,4) NOT NULL DEFAULT 0, updated_at DATETIME NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS inventory_reservation (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_order_id BIGINT NOT NULL,
                  factory_order_id VARCHAR(50) NOT NULL, hardware_item_id BIGINT NOT NULL,
                  required_quantity DECIMAL(14,4) NOT NULL, reserved_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
                  shortage_quantity DECIMAL(14,4) NOT NULL DEFAULT 0, status VARCHAR(30) NOT NULL DEFAULT '待下料锁定',
                  created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL,
                  KEY idx_inventory_reservation_order (customer_order_id)
                )
                """
        );
        ddl.forEach(jdbcTemplate::execute);
        jdbcTemplate.update("""
                INSERT IGNORE INTO company_profile(id,company_name,logo_path,company_address,contact_phone,updated_at)
                VALUES(1,'龙泽伟尼',NULL,NULL,NULL,NOW())
                """);
    }

    private void addFullQuoteColumns() {
        addColumn("factory_order_quote_item", "material_structure", "VARCHAR(200) NULL");
        addColumn("factory_order_quote_item", "handle_color", "VARCHAR(100) NULL");
        addColumn("factory_order_quote_item", "width_mm", "DECIMAL(12,2) NULL");
        addColumn("factory_order_quote_item", "height_mm", "DECIMAL(12,2) NULL");
        addColumn("factory_order_quote_item", "thickness_mm", "DECIMAL(12,2) NULL");
        addColumn("factory_order_quote_item", "hinge_hole", "VARCHAR(100) NULL");
        addColumn("factory_order_quote_item", "process_desc", "VARCHAR(500) NULL");
        addColumn("factory_order_quote_item", "attachment_name", "VARCHAR(200) NULL");
        addColumn("factory_order_quote_item", "attachment_path", "VARCHAR(500) NULL");
        addColumn("factory_order_quote_item", "area_m2", "DECIMAL(12,4) NOT NULL DEFAULT 0");
        addColumn("factory_order_quote_item", "base_unit_price", "DECIMAL(14,4) NOT NULL DEFAULT 0");
        addColumn("factory_order_quote_item", "special_adjust_total", "DECIMAL(14,2) NOT NULL DEFAULT 0");
        addColumn("factory_order_quote_item", "final_unit_price", "DECIMAL(14,4) NOT NULL DEFAULT 0");
        addColumn("factory_order_quote_item", "selected_rule_ids", "VARCHAR(500) NULL");
        addColumn("factory_order_quote_item", "custom_rule_json", "JSON NULL");
        addColumn("factory_order_quote_item", "production_process", "VARCHAR(200) NULL");
        addColumn("factory_order_quote_item", "technician", "VARCHAR(100) NULL");
        addColumn("customer_quote_confirmation", "status", "VARCHAR(30) NOT NULL DEFAULT '有效'");
        addColumn("customer_quote_confirmation", "pdf_id", "BIGINT NULL");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS factory_order_quote_item_extra_price (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT, quote_item_id BIGINT NOT NULL, quote_id BIGINT NOT NULL,
                  source_rule_id BIGINT NULL, rule_name VARCHAR(100) NOT NULL, adjust_mode VARCHAR(50) NOT NULL,
                  adjust_value DECIMAL(14,4) NOT NULL, unit_desc VARCHAR(50) NULL, rule_quantity DECIMAL(12,4) NULL,
                  final_charge DECIMAL(14,2) NOT NULL, discount_eligible TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL, KEY idx_v3_quote_extra_item (quote_item_id),
                  KEY idx_v3_quote_extra_quote (quote_id)
                )
                """);
    }

    private void addColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void addUniqueIndex(String table, String index, String columns) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                """, Integer.class, table, index);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD UNIQUE INDEX " + index + " (" + columns + ")");
        }
    }
}

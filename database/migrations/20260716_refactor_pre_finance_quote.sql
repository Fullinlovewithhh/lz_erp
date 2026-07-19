-- Pre-finance formal order and quotation structure.
-- Safe to run repeatedly on MySQL 8 when columns/tables are not already present.

ALTER TABLE production_line
  ADD COLUMN IF NOT EXISTS quote_mode VARCHAR(20) NOT NULL DEFAULT 'ERP' AFTER line_name;
UPDATE production_line SET quote_mode='ERP' WHERE line_code='L';
UPDATE production_line SET quote_mode='EXTERNAL' WHERE line_code='V';

ALTER TABLE customer
  ADD COLUMN IF NOT EXISTS default_discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1.0000;

ALTER TABLE product
  ADD COLUMN IF NOT EXISTS pricing_mode VARCHAR(20) NOT NULL DEFAULT 'AREA',
  ADD COLUMN IF NOT EXISTS dimension_mode VARCHAR(30) NOT NULL DEFAULT 'WIDTH_HEIGHT',
  ADD COLUMN IF NOT EXISTS min_bill_quantity DECIMAL(12,4) NULL,
  ADD COLUMN IF NOT EXISTS discount_eligible TINYINT NOT NULL DEFAULT 1;

ALTER TABLE customer_order_cad_file
  ADD COLUMN IF NOT EXISTS version_no INT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS is_current TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS version_remark VARCHAR(500) NULL;

ALTER TABLE factory_order_quote_item
  ADD COLUMN IF NOT EXISTS length_mm DECIMAL(12,2) NULL,
  ADD COLUMN IF NOT EXISTS pricing_mode VARCHAR(20) NOT NULL DEFAULT 'AREA',
  ADD COLUMN IF NOT EXISTS billing_quantity DECIMAL(12,4) NOT NULL DEFAULT 0;

ALTER TABLE factory_order_quote_item_extra_price
  ADD COLUMN IF NOT EXISTS rule_category VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS charge_reason VARCHAR(500) NULL,
  ADD COLUMN IF NOT EXISTS is_custom TINYINT NOT NULL DEFAULT 0;

ALTER TABLE customer_quote_pdf
  ADD COLUMN IF NOT EXISTS external_quote_amount DECIMAL(14,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS factory_order_external_quote (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  factory_order_id VARCHAR(50) NOT NULL,
  version_no INT NOT NULL,
  external_quote_no VARCHAR(100) NULL,
  final_amount DECIMAL(14,2) NOT NULL,
  quote_date DATE NOT NULL,
  attachment_path VARCHAR(500) NULL,
  confirmation_status VARCHAR(30) NOT NULL DEFAULT '待客户确认',
  confirmed_at DATETIME NULL,
  recorded_by BIGINT NOT NULL,
  recorded_at DATETIME NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '有效',
  remark VARCHAR(500) NULL,
  UNIQUE KEY uk_external_quote_version (factory_order_id,version_no),
  KEY idx_external_quote_order (factory_order_id),
  CONSTRAINT fk_external_quote_order FOREIGN KEY (factory_order_id) REFERENCES factory_order(factory_order_id)
);

CREATE TABLE IF NOT EXISTS customer_discount_policy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  status VARCHAR(30) NOT NULL DEFAULT '已批准',
  approved_by BIGINT NULL,
  approved_at DATETIME NULL,
  remark VARCHAR(500) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_discount_policy_customer (customer_id,effective_from),
  CONSTRAINT fk_discount_policy_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS factory_order_discount_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  factory_order_id VARCHAR(50) NOT NULL,
  original_rate DECIMAL(6,4) NOT NULL,
  requested_rate DECIMAL(6,4) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待审批',
  requested_by BIGINT NOT NULL,
  requested_at DATETIME NOT NULL,
  approved_by BIGINT NULL,
  approved_at DATETIME NULL,
  approval_remark VARCHAR(500) NULL,
  KEY idx_discount_request_order (factory_order_id,status),
  CONSTRAINT fk_discount_request_order FOREIGN KEY (factory_order_id) REFERENCES factory_order(factory_order_id)
);

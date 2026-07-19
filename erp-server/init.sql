CREATE DATABASE IF NOT EXISTS gc_erp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE gc_erp;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS contract (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_no VARCHAR(50) NOT NULL UNIQUE,
  customer_id BIGINT NULL,
  project_id BIGINT NULL,
  customer_order_id BIGINT NULL,
  factory_order_no VARCHAR(50) NULL,
  customer_order_no VARCHAR(255) NULL,
  customer_name_snapshot VARCHAR(100) NULL,
  customer_phone_snapshot VARCHAR(30) NULL,
  customer_address_snapshot VARCHAR(255) NULL,
  project_name_snapshot VARCHAR(100) NULL,
  customer_name VARCHAR(100) NOT NULL,
  customer_phone VARCHAR(30) NOT NULL,
  customer_address VARCHAR(255) NULL,
  demand_desc TEXT NOT NULL,
  status VARCHAR(50) NOT NULL,
  production_progress INT NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_contract_customer_id (customer_id),
  KEY idx_contract_project_id (project_id),
  KEY idx_contract_customer_order_id (customer_order_id),
  KEY idx_contract_factory_order_no (factory_order_no),
  KEY idx_contract_customer_order_no (customer_order_no),
  UNIQUE KEY uk_contract_factory_order_no (factory_order_no)
);

CREATE TABLE IF NOT EXISTS workflow_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  from_status VARCHAR(50) NULL,
  to_status VARCHAR(50) NOT NULL,
  operator VARCHAR(100) NOT NULL,
  remark VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  KEY idx_contract_id (contract_id)
);

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  module VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL,
  operator VARCHAR(100) NOT NULL,
  target_type VARCHAR(100) NULL,
  target_id VARCHAR(100) NULL,
  before_data LONGTEXT NULL,
  after_data LONGTEXT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  KEY idx_operation_module (module),
  KEY idx_operation_operator (operator),
  KEY idx_operation_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS quotation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  assignment_id BIGINT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  quote_desc VARCHAR(500) NULL,
  created_by VARCHAR(100) NOT NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  KEY idx_quotation_contract_id (contract_id)
);

CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  pay_status VARCHAR(50) NOT NULL,
  pay_channel VARCHAR(100) NULL,
  checked_by VARCHAR(100) NOT NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  checked_at DATETIME NOT NULL,
  KEY idx_payment_contract_id (contract_id)
);

CREATE TABLE IF NOT EXISTS production_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  task_no VARCHAR(50) NOT NULL UNIQUE,
  process_plan VARCHAR(1000) NOT NULL,
  assignee VARCHAR(100) NULL,
  task_status VARCHAR(50) NOT NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_production_contract_id (contract_id)
);

CREATE TABLE IF NOT EXISTS customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_code VARCHAR(50) NOT NULL UNIQUE,
  customer_name VARCHAR(100) NOT NULL,
  phone VARCHAR(30) NULL,
  address VARCHAR(255) NULL,
  level VARCHAR(50) NULL,
  owner VARCHAR(100) NULL,
  default_discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  customer_order_no VARCHAR(255) NOT NULL,
  customer_order_name VARCHAR(255) NULL,
  service_staff_id BIGINT NULL,
  review_engineer_id BIGINT NULL,
  review_status VARCHAR(30) NOT NULL DEFAULT '待领取',
  split_instruction TEXT NULL,
  settlement_type VARCHAR(30) NULL,
  quote_status VARCHAR(30) NOT NULL DEFAULT '待拆单',
  quote_total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  price_adjustment_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  final_receivable_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  payment_status VARCHAR(30) NOT NULL DEFAULT '未收款',
  cutting_release_status VARCHAR(30) NOT NULL DEFAULT '未申请',
  tax_rate DECIMAL(6,4) NOT NULL DEFAULT 0,
  quote_valid_days INT NOT NULL DEFAULT 15,
  remark TEXT NULL,
  status VARCHAR(50) NOT NULL DEFAULT '启用',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_customer_order_customer_id (customer_id),
  KEY idx_customer_order_no (customer_order_no),
  UNIQUE KEY uk_customer_order_no (customer_order_no)
);

CREATE TABLE IF NOT EXISTS customer_followup (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  next_plan VARCHAR(500) NULL,
  creator VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_customer_followup_customer_id (customer_id)
);

CREATE TABLE IF NOT EXISTS customer_requirement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  requirement_type VARCHAR(100) NOT NULL,
  requirement_desc TEXT NOT NULL,
  budget_range VARCHAR(100) NULL,
  style_preference VARCHAR(100) NULL,
  creator VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_customer_requirement_customer_id (customer_id)
);

CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_code VARCHAR(50) NOT NULL UNIQUE,
  type VARCHAR(100) NULL,
  product_name VARCHAR(100) NOT NULL,
  model VARCHAR(100) NULL,
  material_name VARCHAR(100) NULL,
  color VARCHAR(100) NULL,
  handle_color VARCHAR(100) NULL,
  unit_price DECIMAL(12,2) NULL,
  unit_price_unit VARCHAR(30) NULL,
  pricing_mode VARCHAR(20) NOT NULL DEFAULT 'AREA' COMMENT 'AREA/LENGTH/COUNT',
  dimension_mode VARCHAR(30) NOT NULL DEFAULT 'WIDTH_HEIGHT',
  min_bill_quantity DECIMAL(12,4) NULL,
  discount_eligible TINYINT NOT NULL DEFAULT 1,
  thickness VARCHAR(50) NULL,
  thickness_unit VARCHAR(30) NULL,
  size VARCHAR(100) NULL,
  image_url VARCHAR(500) NULL,
  unit VARCHAR(30) NULL,
  status VARCHAR(50) NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_code VARCHAR(50) NOT NULL UNIQUE,
  material_name VARCHAR(100) NOT NULL,
  material_type VARCHAR(100) NULL,
  color VARCHAR(100) NULL,
  length_mm DECIMAL(12,2) NULL,
  width_mm DECIMAL(12,2) NULL,
  thickness_mm DECIMAL(12,2) NULL,
  image_url VARCHAR(500) NULL,
  unit VARCHAR(30) NULL,
  status VARCHAR(50) NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS staff (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  staff_code VARCHAR(50) NOT NULL UNIQUE,
  staff_name VARCHAR(100) NOT NULL,
  role_type VARCHAR(100) NOT NULL,
  process_name VARCHAR(100) NULL,
  phone VARCHAR(30) NULL,
  status VARCHAR(50) NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

INSERT IGNORE INTO staff(
  staff_code, staff_name, role_type, process_name, phone, status, custom_fields, custom_text1, custom_text2, custom_text3, created_at, updated_at
) VALUES
('KF001','张客服','客服',NULL,'13800010001','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('KF002','李客服','客服',NULL,'13800010002','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('KF003','王客服','客服',NULL,'13800010003','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('SH001','陈深化','深化设计师',NULL,'13800020001','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('SH002','赵深化','深化设计师',NULL,'13800020002','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('SH003','孙深化','深化设计师',NULL,'13800020003','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('XD001','周下单','下单员',NULL,'13800030001','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('XD002','吴下单','下单员',NULL,'13800030002','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('PC001','郑排产','排产员',NULL,'13800040001','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('PC002','冯排产','排产员',NULL,'13800040002','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('CJ001','褚车间','车间','封边','13800050001','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('CJ002','卫车间','车间','打孔','13800050002','启用',NULL,NULL,NULL,NULL,NOW(),NOW()),
('CJ003','蒋车间','车间','包装','13800050003','启用',NULL,NULL,NULL,NULL,NOW(),NOW());

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_def TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_def);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_column_if_missing('product', 'model', 'VARCHAR(100) NULL');
CALL add_column_if_missing('product', 'type', 'VARCHAR(100) NULL');
CALL add_column_if_missing('product', 'material_name', 'VARCHAR(100) NULL');
CALL add_column_if_missing('product', 'color', 'VARCHAR(100) NULL');
CALL add_column_if_missing('product', 'handle_color', 'VARCHAR(100) NULL');
CALL add_column_if_missing('product', 'unit_price', 'DECIMAL(12,2) NULL');
CALL add_column_if_missing('product', 'unit_price_unit', 'VARCHAR(30) NULL');
CALL add_column_if_missing('product', 'thickness', 'VARCHAR(50) NULL');
CALL add_column_if_missing('product', 'thickness_unit', 'VARCHAR(30) NULL');
CALL add_column_if_missing('product', 'size', 'VARCHAR(100) NULL');
CALL add_column_if_missing('product', 'image_url', 'VARCHAR(500) NULL');

CALL add_column_if_missing('material', 'color', 'VARCHAR(100) NULL');
CALL add_column_if_missing('material', 'length_mm', 'DECIMAL(12,2) NULL');
CALL add_column_if_missing('material', 'width_mm', 'DECIMAL(12,2) NULL');
CALL add_column_if_missing('material', 'thickness_mm', 'DECIMAL(12,2) NULL');
CALL add_column_if_missing('material', 'image_url', 'VARCHAR(500) NULL');

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS process_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(50) NOT NULL UNIQUE,
  rule_name VARCHAR(100) NOT NULL,
  rule_type VARCHAR(100) NULL,
  rule_content TEXT NULL,
  status VARCHAR(50) NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS price_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(50) NOT NULL UNIQUE,
  rule_name VARCHAR(100) NOT NULL,
  price_type VARCHAR(100) NULL,
  price_value DECIMAL(12,2) NULL,
  status VARCHAR(50) NULL,
  custom_fields JSON NULL,
  custom_text1 VARCHAR(255) NULL,
  custom_text2 VARCHAR(255) NULL,
  custom_text3 VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS quote_special_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_code VARCHAR(50) NOT NULL UNIQUE,
  rule_category VARCHAR(100) NULL,
  rule_name VARCHAR(200) NOT NULL,
  adjust_mode VARCHAR(50) NOT NULL COMMENT 'FIXED_PER_M2/PERCENT/FIXED_PER_ITEM',
  adjust_value DECIMAL(12,2) NOT NULL,
  unit_desc VARCHAR(30) NULL,
  min_area_m2 DECIMAL(12,4) NULL,
  min_charge DECIMAL(12,2) NULL,
  max_charge DECIMAL(12,2) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_quote_special_rule_name (rule_name)
);

CREATE TABLE IF NOT EXISTS quote_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  contract_no VARCHAR(50) NOT NULL,
  service_staff VARCHAR(100) NULL,
  engineer VARCHAR(100) NOT NULL,
  status VARCHAR(50) NOT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_quote_assignment_contract_id (contract_id),
  KEY idx_quote_assignment_engineer (engineer)
);

INSERT IGNORE INTO quote_special_rule(rule_code, rule_category, rule_name, adjust_mode, adjust_value, unit_desc, min_area_m2, min_charge, max_charge, enabled, remark, created_at, updated_at) VALUES
('QR0001','柜门','2700<板材尺寸≤3000(mm)','FIXED_PER_M2',100,'元/平',NULL,NULL,NULL,1,'在2700mm板相应规格基础上加价',NOW(),NOW()),
('QR0002','柜门','铝蜂窝板基材(26mm厚)3600mm','FIXED_PER_M2',1000,'元/平',1.35,NULL,NULL,1,'不足1.35按1.35平核算',NOW(),NOW()),
('QR0003','柜门','非标色块','FIXED_PER_ITEM',200,'元/块',NULL,NULL,NULL,1,'按块计费，可结合颜色附加使用',NOW(),NOW()),
('QR0004','柜门','非标颜色百分比附加','PERCENT',10,'%',NULL,500,1500,1,'整单非标颜色费用下限500，上限1500',NOW(),NOW()),
('QR0005','柜门','门型样块','FIXED_PER_M2',0,'元/平',0.3,NULL,NULL,1,'按平计价，不足0.3平按0.3平核算',NOW(),NOW()),
('QR0006','柜门','实色封闭漆磨砂效果','FIXED_PER_M2',50,'元/平',NULL,NULL,NULL,1,'在对应产品基价加价',NOW(),NOW()),
('QR0007','柜门','水性漆','FIXED_PER_M2',60,'元/平',NULL,NULL,NULL,1,'水性面漆',NOW(),NOW()),
('QR0008','柜门','可修复水性面漆/肤感水性面漆','FIXED_PER_M2',150,'元/平',NULL,NULL,NULL,1,'在水性漆基础上的高级工艺',NOW(),NOW()),
('QR0009','柜门','橡木直纹','FIXED_PER_M2',50,'元/平',NULL,NULL,NULL,1,'在对应产品基价加价',NOW(),NOW()),
('QR0010','柜门','黑胡桃直纹','FIXED_PER_M2',100,'元/平',NULL,NULL,NULL,1,'在对应产品基价加价',NOW(),NOW()),
('QR0011','柜门','木皮纹理斜纹/鱼骨纹(单面)','FIXED_PER_M2',400,'元/平',NULL,NULL,NULL,1,'其他拼花按实际工艺定价',NOW(),NOW());

CREATE TABLE IF NOT EXISTS quote_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id BIGINT NULL,
  product_name VARCHAR(200) NOT NULL,
  material_structure VARCHAR(200) NULL,
  handle_color VARCHAR(100) NULL,
  width_mm DECIMAL(12,2) NOT NULL,
  height_mm DECIMAL(12,2) NOT NULL,
  thickness_mm DECIMAL(12,2) NULL,
  quantity INT NOT NULL,
  hinge_hole VARCHAR(100) NULL,
  process_desc VARCHAR(500) NULL,
  attachment_name VARCHAR(200) NULL,
  unit VARCHAR(20) NOT NULL DEFAULT 'm²',
  area_m2 DECIMAL(12,4) NOT NULL,
  base_unit_price DECIMAL(12,2) NOT NULL,
  special_adjust_total DECIMAL(12,2) NOT NULL,
  final_unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  selected_rule_ids VARCHAR(500) NULL,
  custom_rule_json JSON NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_quote_detail_quote_id (quote_id)
);

DROP PROCEDURE IF EXISTS add_quote_detail_handle_color_if_missing;
DELIMITER //
CREATE PROCEDURE add_quote_detail_handle_color_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'quote_detail'
      AND COLUMN_NAME = 'handle_color'
  ) THEN
    ALTER TABLE quote_detail ADD COLUMN handle_color VARCHAR(100) NULL AFTER material_structure;
  END IF;
END//
DELIMITER ;
CALL add_quote_detail_handle_color_if_missing();
DROP PROCEDURE IF EXISTS add_quote_detail_handle_color_if_missing;

CREATE TABLE IF NOT EXISTS quote_detail_extra_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  detail_id BIGINT NOT NULL,
  quote_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  rule_name VARCHAR(100) NOT NULL,
  adjust_mode VARCHAR(50) NOT NULL,
  adjust_value DECIMAL(12,2) NOT NULL,
  unit_desc VARCHAR(50) NULL,
  min_area_m2 DECIMAL(12,4) NULL,
  min_charge DECIMAL(12,2) NULL,
  max_charge DECIMAL(12,2) NULL,
  rule_quantity DECIMAL(12,2) NULL,
  final_charge DECIMAL(12,2) NOT NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_quote_detail_extra_detail_id (detail_id),
  KEY idx_quote_detail_extra_quote_id (quote_id)
);

CREATE TABLE IF NOT EXISTS contract_cad_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  contract_no VARCHAR(50) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(20) NULL,
  file_size BIGINT NULL,
  file_path VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_contract_cad_contract_id (contract_id)
);

CREATE TABLE IF NOT EXISTS finance_voucher (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  contract_id BIGINT NOT NULL,
  contract_no VARCHAR(50) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(20) NULL,
  file_size BIGINT NULL,
  file_path VARCHAR(500) NOT NULL,
  created_by VARCHAR(100) NULL,
  created_at DATETIME NOT NULL,
  KEY idx_finance_voucher_contract_id (contract_id)
);

-- 为已存在旧表补齐自定义字段（兼容不支持 ADD COLUMN IF NOT EXISTS 的版本）
SET @db_name = DATABASE();

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE contract ADD COLUMN production_progress INT NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'production_progress'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 系统登录账号（账号密码 + 身份权限）
CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(80) NOT NULL,
  password_salt VARCHAR(50) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role_code VARCHAR(30) NOT NULL COMMENT 'ADMIN/SERVICE/ENGINEER/FINANCE/PRODUCTION',
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_app_user_role (role_code)
);

INSERT IGNORE INTO app_user(username, password_hash, password_salt, display_name, role_code, enabled, created_at, updated_at) VALUES
(
  'admin',
  CONCAT(MD5(CONCAT('admin123','LZWN_ADMIN')), MD5(CONCAT('sha2|', CONCAT('admin123','LZWN_ADMIN')))),
  'LZWN_ADMIN',
  '系统管理员',
  'ADMIN',
  1,
  NOW(),
  NOW()
),
(
  'kf001',
  CONCAT(MD5(CONCAT('123456','LZWN_KF001')), MD5(CONCAT('sha2|', CONCAT('123456','LZWN_KF001')))),
  'LZWN_KF001',
  '客服账号',
  'SERVICE',
  1,
  NOW(),
  NOW()
),
(
  'sh001',
  CONCAT(MD5(CONCAT('123456','LZWN_SH001')), MD5(CONCAT('sha2|', CONCAT('123456','LZWN_SH001')))),
  'LZWN_SH001',
  '深化工程师账号',
  'ENGINEER',
  1,
  NOW(),
  NOW()
),
(
  'cw001',
  CONCAT(MD5(CONCAT('123456','LZWN_CW001')), MD5(CONCAT('sha2|', CONCAT('123456','LZWN_CW001')))),
  'LZWN_CW001',
  '财务账号',
  'FINANCE',
  1,
  NOW(),
  NOW()
),
(
  'pc001',
  CONCAT(MD5(CONCAT('123456','LZWN_PC001')), MD5(CONCAT('sha2|', CONCAT('123456','LZWN_PC001')))),
  'LZWN_PC001',
  '生产账号',
  'PRODUCTION',
  1,
  NOW(),
  NOW()
),
(
  'director001',
  CONCAT(MD5(CONCAT('123456','LZWN_DIRECTOR001')), MD5(CONCAT('sha2|', CONCAT('123456','LZWN_DIRECTOR001')))),
  'LZWN_DIRECTOR001',
  '厂长账号',
  'DIRECTOR',
  1,
  NOW(),
  NOW()
);

-- 兼容历史英文状态，统一映射为当前中文流程状态（用于订单状态、财务确认等页面）
UPDATE contract SET status = '报价中' WHERE status IN ('PENDING_DESIGN');
UPDATE contract SET status = '待财务确认' WHERE status IN ('PENDING_QUOTE', 'QUOTING');
UPDATE contract SET status = '待客户确认' WHERE status IN ('PENDING_CONFIRM', 'WAIT_CUSTOMER_CONFIRM');
UPDATE contract SET status = '待财务确认' WHERE status IN ('PENDING_PAYMENT', 'WAIT_PAYMENT', 'PENDING_FINANCE_CONFIRM');
UPDATE contract SET status = '待财务确认' WHERE status = '待客服付款';
UPDATE contract SET status = '财务已收款' WHERE status IN ('FINANCE_CONFIRMED', 'PAID', 'PAYMENT_CONFIRMED');
UPDATE contract SET status = '待排产' WHERE status IN ('PENDING_PRODUCTION', 'TO_PRODUCTION');
UPDATE contract SET status = '生产中' WHERE status IN ('IN_PRODUCTION', 'PRODUCING');
UPDATE contract SET status = '已完成' WHERE status IN ('COMPLETED', 'DONE');
UPDATE contract SET status = '待发货' WHERE status IN ('PENDING_SHIPMENT', 'WAIT_SHIP');
UPDATE contract SET status = '已发货' WHERE status IN ('SHIPPED');

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE contract ADD COLUMN custom_fields JSON NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'custom_fields'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE contract ADD COLUMN custom_text1 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'custom_text1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE contract ADD COLUMN custom_text2 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'custom_text2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE contract ADD COLUMN custom_text3 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'custom_text3'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quotation ADD COLUMN custom_fields JSON NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quotation' AND COLUMN_NAME = 'custom_fields'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quotation ADD COLUMN custom_text1 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quotation' AND COLUMN_NAME = 'custom_text1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quotation ADD COLUMN custom_text2 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quotation' AND COLUMN_NAME = 'custom_text2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quotation ADD COLUMN custom_text3 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quotation' AND COLUMN_NAME = 'custom_text3'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE payment_record ADD COLUMN custom_fields JSON NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payment_record' AND COLUMN_NAME = 'custom_fields'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE payment_record ADD COLUMN custom_text1 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payment_record' AND COLUMN_NAME = 'custom_text1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE payment_record ADD COLUMN custom_text2 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payment_record' AND COLUMN_NAME = 'custom_text2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE payment_record ADD COLUMN custom_text3 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'payment_record' AND COLUMN_NAME = 'custom_text3'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE production_task ADD COLUMN custom_fields JSON NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'production_task' AND COLUMN_NAME = 'custom_fields'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE production_task ADD COLUMN custom_text1 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'production_task' AND COLUMN_NAME = 'custom_text1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE production_task ADD COLUMN custom_text2 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'production_task' AND COLUMN_NAME = 'custom_text2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quote_special_rule ADD COLUMN rule_category VARCHAR(100) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quote_special_rule' AND COLUMN_NAME = 'rule_category'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quote_special_rule ADD COLUMN unit_desc VARCHAR(30) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quote_special_rule' AND COLUMN_NAME = 'unit_desc'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quote_special_rule ADD COLUMN min_area_m2 DECIMAL(12,4) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quote_special_rule' AND COLUMN_NAME = 'min_area_m2'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quote_special_rule ADD COLUMN min_charge DECIMAL(12,2) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quote_special_rule' AND COLUMN_NAME = 'min_charge'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quote_special_rule ADD COLUMN max_charge DECIMAL(12,2) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quote_special_rule' AND COLUMN_NAME = 'max_charge'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE quotation ADD COLUMN assignment_id BIGINT NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'quotation' AND COLUMN_NAME = 'assignment_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE production_task ADD COLUMN custom_text3 VARCHAR(255) NULL',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'production_task' AND COLUMN_NAME = 'custom_text3'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS customer_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  customer_order_no VARCHAR(255) NOT NULL,
  customer_order_name VARCHAR(255) NULL,
  service_staff_id BIGINT NULL,
  review_engineer_id BIGINT NULL,
  review_status VARCHAR(30) NOT NULL DEFAULT '待领取',
  split_instruction TEXT NULL,
  settlement_type VARCHAR(30) NULL,
  quote_status VARCHAR(30) NOT NULL DEFAULT '待拆单',
  quote_total_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  price_adjustment_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  final_receivable_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  payment_status VARCHAR(30) NOT NULL DEFAULT '未收款',
  cutting_release_status VARCHAR(30) NOT NULL DEFAULT '未申请',
  tax_rate DECIMAL(6,4) NOT NULL DEFAULT 0,
  quote_valid_days INT NOT NULL DEFAULT 15,
  remark TEXT NULL,
  status VARCHAR(50) NOT NULL DEFAULT '启用',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_customer_order_customer_id (customer_id),
  KEY idx_customer_order_no (customer_order_no),
  UNIQUE KEY uk_customer_order_no (customer_order_no)
);

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN customer_id BIGINT NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'customer_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN project_id BIGINT NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'project_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN customer_order_id BIGINT NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'customer_order_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN factory_order_no VARCHAR(50) NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'factory_order_no'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN customer_order_no VARCHAR(255) NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'customer_order_no'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN customer_name_snapshot VARCHAR(100) NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'customer_name_snapshot'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN customer_phone_snapshot VARCHAR(30) NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'customer_phone_snapshot'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN customer_address_snapshot VARCHAR(255) NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'customer_address_snapshot'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0, 'ALTER TABLE contract ADD COLUMN project_name_snapshot VARCHAR(100) NULL', 'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contract' AND COLUMN_NAME = 'project_name_snapshot'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE contract
SET factory_order_no = contract_no
WHERE factory_order_no IS NULL OR factory_order_no = '';

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_index_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_index_name VARCHAR(64),
  IN p_index_def TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND INDEX_NAME = p_index_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_table_name, ' ADD ', p_index_def);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_index_if_missing('contract', 'idx_contract_customer_id', 'INDEX idx_contract_customer_id (customer_id)');
CALL add_index_if_missing('contract', 'idx_contract_project_id', 'INDEX idx_contract_project_id (project_id)');
CALL add_index_if_missing('contract', 'idx_contract_customer_order_id', 'INDEX idx_contract_customer_order_id (customer_order_id)');
CALL add_index_if_missing('contract', 'idx_contract_factory_order_no', 'INDEX idx_contract_factory_order_no (factory_order_no)');
CALL add_index_if_missing('contract', 'idx_contract_customer_order_no', 'INDEX idx_contract_customer_order_no (customer_order_no)');
CALL add_index_if_missing('contract', 'uk_contract_factory_order_no', 'UNIQUE INDEX uk_contract_factory_order_no (factory_order_no)');
CALL add_index_if_missing('customer_order', 'idx_customer_order_customer_id', 'INDEX idx_customer_order_customer_id (customer_id)');
CALL add_index_if_missing('customer_order', 'idx_customer_order_no', 'INDEX idx_customer_order_no (customer_order_no)');
CALL add_index_if_missing('customer_order', 'uk_customer_order_no', 'UNIQUE INDEX uk_customer_order_no (customer_order_no)');

DROP PROCEDURE IF EXISTS add_index_if_missing;

INSERT INTO customer_order(customer_id, customer_order_no, customer_order_name, status, is_deleted, created_at, updated_at)
SELECT DISTINCT c.customer_id,
                c.customer_order_no,
                c.customer_order_no,
                '启用',
                0,
                NOW(),
                NOW()
FROM contract c
LEFT JOIN customer_order co
  ON co.customer_id = c.customer_id
 AND co.customer_order_no = c.customer_order_no
 AND co.is_deleted = 0
WHERE c.customer_id IS NOT NULL
  AND c.customer_order_no IS NOT NULL
  AND c.customer_order_no <> ''
  AND co.id IS NULL;

UPDATE contract c
JOIN customer_order co
  ON co.customer_id = c.customer_id
 AND co.customer_order_no = c.customer_order_no
 AND co.is_deleted = 0
SET c.customer_order_id = co.id
WHERE c.customer_order_id IS NULL
  AND c.customer_order_no IS NOT NULL
  AND c.customer_order_no <> '';

-- ERP V3 formal order, quotation and finance structure.
CREATE TABLE IF NOT EXISTS production_line (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  line_code VARCHAR(10) NOT NULL,
  line_name VARCHAR(100) NOT NULL,
  quote_mode VARCHAR(20) NOT NULL DEFAULT 'ERP' COMMENT 'ERP/EXTERNAL/NONE',
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_production_line_code (line_code)
);

INSERT IGNORE INTO production_line(line_code,line_name,quote_mode,enabled,sort_order,created_at,updated_at) VALUES
('L','实木生产线','ERP',1,10,NOW(),NOW()),
('V','板式生产线','EXTERNAL',1,20,NOW(),NOW());

CREATE TABLE IF NOT EXISTS customer_order_cad_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(20) NULL,
  file_size BIGINT NULL,
  file_path VARCHAR(500) NOT NULL,
  version_no INT NOT NULL DEFAULT 1,
  is_current TINYINT NOT NULL DEFAULT 1,
  version_remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_customer_order_cad_order (customer_order_id),
  CONSTRAINT fk_customer_order_cad_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id)
);

CREATE TABLE IF NOT EXISTS customer_order_split_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  factory_order_name VARCHAR(255) NOT NULL,
  production_line_id BIGINT NOT NULL,
  order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  parent_factory_order_id VARCHAR(50) NULL,
  remark VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT '草稿',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_split_draft_order (customer_order_id),
  CONSTRAINT fk_split_draft_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id),
  CONSTRAINT fk_split_draft_line FOREIGN KEY (production_line_id) REFERENCES production_line(id)
);

CREATE TABLE IF NOT EXISTS factory_order (
  factory_order_id VARCHAR(50) PRIMARY KEY,
  customer_order_id BIGINT NOT NULL,
  production_line_id BIGINT NOT NULL,
  factory_order_name VARCHAR(255) NOT NULL,
  order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  parent_factory_order_id VARCHAR(50) NULL,
  split_sequence INT NOT NULL,
  quote_assignee_id BIGINT NULL,
  quote_assignee_role VARCHAR(30) NULL,
  status VARCHAR(50) NOT NULL DEFAULT '待报价分配',
  discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  original_quote_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  discounted_quote_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  current_quote_version INT NOT NULL DEFAULT 0,
  supplement_credit_flag TINYINT NOT NULL DEFAULT 0,
  demand_desc TEXT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_factory_order_split_seq (customer_order_id, split_sequence),
  KEY idx_factory_order_customer_order (customer_order_id),
  KEY idx_factory_order_assignee (quote_assignee_id),
  KEY idx_factory_order_parent (parent_factory_order_id),
  CONSTRAINT fk_factory_order_customer_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id),
  CONSTRAINT fk_factory_order_line FOREIGN KEY (production_line_id) REFERENCES production_line(id),
  CONSTRAINT fk_factory_order_parent FOREIGN KEY (parent_factory_order_id) REFERENCES factory_order(factory_order_id)
);

CREATE TABLE IF NOT EXISTS factory_order_assignment_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  factory_order_id VARCHAR(50) NOT NULL,
  from_user_id BIGINT NULL,
  to_user_id BIGINT NULL,
  action_type VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL,
  operated_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  KEY idx_assignment_log_order (factory_order_id),
  CONSTRAINT fk_assignment_log_order FOREIGN KEY (factory_order_id) REFERENCES factory_order(factory_order_id)
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

CREATE TABLE IF NOT EXISTS factory_order_quote (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  factory_order_id VARCHAR(50) NOT NULL,
  version_no INT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '草稿',
  original_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  discount_rate DECIMAL(6,4) NOT NULL DEFAULT 1.0000,
  discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  final_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  quote_desc VARCHAR(500) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  submitted_at DATETIME NULL,
  UNIQUE KEY uk_factory_quote_version (factory_order_id, version_no),
  KEY idx_factory_quote_order (factory_order_id),
  CONSTRAINT fk_factory_quote_order FOREIGN KEY (factory_order_id) REFERENCES factory_order(factory_order_id)
);

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
  UNIQUE KEY uk_external_quote_version (factory_order_id, version_no),
  KEY idx_external_quote_order (factory_order_id),
  CONSTRAINT fk_external_quote_order FOREIGN KEY (factory_order_id) REFERENCES factory_order(factory_order_id)
);

CREATE TABLE IF NOT EXISTS factory_order_quote_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id BIGINT NOT NULL,
  product_category VARCHAR(50) NOT NULL,
  product_id BIGINT NULL,
  hardware_item_id BIGINT NULL,
  product_name VARCHAR(200) NOT NULL,
  specification VARCHAR(500) NULL,
  material_structure VARCHAR(200) NULL,
  handle_color VARCHAR(100) NULL,
  width_mm DECIMAL(12,2) NULL,
  height_mm DECIMAL(12,2) NULL,
  length_mm DECIMAL(12,2) NULL,
  thickness_mm DECIMAL(12,2) NULL,
  hinge_hole VARCHAR(100) NULL,
  process_desc VARCHAR(500) NULL,
  attachment_name VARCHAR(200) NULL,
  attachment_path VARCHAR(500) NULL,
  area_m2 DECIMAL(12,4) NOT NULL DEFAULT 0,
  pricing_mode VARCHAR(20) NOT NULL DEFAULT 'AREA',
  billing_quantity DECIMAL(12,4) NOT NULL DEFAULT 0,
  base_unit_price DECIMAL(14,4) NOT NULL DEFAULT 0,
  special_adjust_total DECIMAL(14,2) NOT NULL DEFAULT 0,
  final_unit_price DECIMAL(14,4) NOT NULL DEFAULT 0,
  selected_rule_ids VARCHAR(500) NULL,
  custom_rule_json JSON NULL,
  production_process VARCHAR(200) NULL,
  technician VARCHAR(100) NULL,
  quantity DECIMAL(12,4) NOT NULL,
  unit VARCHAR(30) NOT NULL,
  unit_price DECIMAL(14,4) NOT NULL,
  original_amount DECIMAL(14,2) NOT NULL,
  discount_eligible TINYINT NOT NULL DEFAULT 1,
  discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  final_amount DECIMAL(14,2) NOT NULL,
  remark VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  KEY idx_factory_quote_item_quote (quote_id),
  CONSTRAINT fk_factory_quote_item_quote FOREIGN KEY (quote_id) REFERENCES factory_order_quote(id)
);

CREATE TABLE IF NOT EXISTS factory_order_quote_item_extra_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_item_id BIGINT NOT NULL,
  quote_id BIGINT NOT NULL,
  source_rule_id BIGINT NULL,
  rule_name VARCHAR(100) NOT NULL,
  rule_category VARCHAR(100) NULL,
  charge_reason VARCHAR(500) NULL,
  is_custom TINYINT NOT NULL DEFAULT 0,
  adjust_mode VARCHAR(50) NOT NULL,
  adjust_value DECIMAL(14,4) NOT NULL,
  unit_desc VARCHAR(50) NULL,
  rule_quantity DECIMAL(12,4) NULL,
  final_charge DECIMAL(14,2) NOT NULL,
  discount_eligible TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  KEY idx_v3_quote_extra_item (quote_item_id),
  KEY idx_v3_quote_extra_quote (quote_id),
  CONSTRAINT fk_v3_quote_extra_item FOREIGN KEY (quote_item_id) REFERENCES factory_order_quote_item(id),
  CONSTRAINT fk_v3_quote_extra_quote FOREIGN KEY (quote_id) REFERENCES factory_order_quote(id)
);

CREATE TABLE IF NOT EXISTS customer_order_price_adjustment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  before_amount DECIMAL(14,2) NOT NULL,
  after_amount DECIMAL(14,2) NOT NULL,
  adjustment_amount DECIMAL(14,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待厂长审批',
  requested_by BIGINT NOT NULL,
  requested_at DATETIME NOT NULL,
  approved_by BIGINT NULL,
  approved_at DATETIME NULL,
  approval_remark VARCHAR(500) NULL,
  KEY idx_price_adjustment_order (customer_order_id),
  CONSTRAINT fk_price_adjustment_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id)
);

CREATE TABLE IF NOT EXISTS customer_quote_confirmation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  pdf_id BIGINT NOT NULL,
  confirmation_version INT NOT NULL,
  confirmation_method VARCHAR(30) NOT NULL,
  confirmed_at DATETIME NOT NULL,
  customer_contact VARCHAR(100) NOT NULL,
  confirmation_remark VARCHAR(500) NULL,
  attachment_path VARCHAR(500) NULL,
  recorded_by BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '有效',
  created_at DATETIME NOT NULL,
  KEY idx_quote_confirmation_order (customer_order_id),
  CONSTRAINT fk_quote_confirmation_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id)
);

CREATE TABLE IF NOT EXISTS company_profile (
  id BIGINT PRIMARY KEY,
  company_name VARCHAR(200) NOT NULL,
  logo_path VARCHAR(500) NULL,
  company_address VARCHAR(500) NULL,
  contact_phone VARCHAR(100) NULL,
  updated_at DATETIME NOT NULL
);

INSERT IGNORE INTO company_profile(id,company_name,logo_path,company_address,contact_phone,updated_at)
VALUES(1,'龙泽伟尼',NULL,NULL,NULL,NOW());

CREATE TABLE IF NOT EXISTS customer_quote_pdf (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  pdf_version INT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待客户确认',
  tax_rate DECIMAL(6,4) NOT NULL DEFAULT 0,
  valid_days INT NOT NULL DEFAULT 15,
  product_craft_original DECIMAL(14,2) NOT NULL,
  product_craft_discount DECIMAL(14,2) NOT NULL,
  non_discount_craft DECIMAL(14,2) NOT NULL,
  hardware_amount DECIMAL(14,2) NOT NULL,
  external_quote_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  price_adjustment DECIMAL(14,2) NOT NULL,
  untaxed_total DECIMAL(14,2) NOT NULL,
  tax_amount DECIMAL(14,2) NOT NULL,
  tax_included_total DECIMAL(14,2) NOT NULL,
  quote_remark VARCHAR(1000) NULL,
  file_path VARCHAR(500) NOT NULL,
  generated_by BIGINT NOT NULL,
  generated_at DATETIME NOT NULL,
  invalidated_at DATETIME NULL,
  UNIQUE KEY uk_customer_quote_pdf_version (customer_order_id,pdf_version),
  KEY idx_customer_quote_pdf_order (customer_order_id),
  CONSTRAINT fk_customer_quote_pdf_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id)
);

CREATE TABLE IF NOT EXISTS receiving_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_code VARCHAR(50) NOT NULL,
  account_name VARCHAR(100) NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  bank_name VARCHAR(100) NULL,
  account_no_masked VARCHAR(100) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_receiving_account_code (account_code)
);

CREATE TABLE IF NOT EXISTS customer_payment_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  plan_version INT NOT NULL,
  installment_no INT NOT NULL,
  planned_amount DECIMAL(14,2) NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待厂长审批',
  customer_confirmed TINYINT NOT NULL DEFAULT 0,
  requested_by BIGINT NOT NULL,
  approved_by BIGINT NULL,
  approved_at DATETIME NULL,
  approval_remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_payment_plan_installment (customer_order_id, plan_version, installment_no),
  KEY idx_payment_plan_due (due_date, status),
  CONSTRAINT fk_payment_plan_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id)
);

CREATE TABLE IF NOT EXISTS customer_payment_receipt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  payment_plan_id BIGINT NULL,
  receiving_account_id BIGINT NULL,
  actual_amount DECIMAL(14,2) NOT NULL,
  received_at DATETIME NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  account_snapshot VARCHAR(255) NULL,
  payer_name VARCHAR(100) NULL,
  bank_reference VARCHAR(255) NULL,
  voucher_path VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待财务确认',
  submitted_by BIGINT NOT NULL,
  confirmed_by BIGINT NULL,
  confirmed_at DATETIME NULL,
  finance_remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL,
  KEY idx_payment_receipt_order (customer_order_id),
  KEY idx_payment_receipt_status (status),
  CONSTRAINT fk_payment_receipt_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id),
  CONSTRAINT fk_payment_receipt_plan FOREIGN KEY (payment_plan_id) REFERENCES customer_payment_plan(id),
  CONSTRAINT fk_payment_receipt_account FOREIGN KEY (receiving_account_id) REFERENCES receiving_account(id)
);

CREATE TABLE IF NOT EXISTS customer_monthly_account (
  customer_id BIGINT PRIMARY KEY,
  enabled TINYINT NOT NULL DEFAULT 0,
  overdue_locked TINYINT NOT NULL DEFAULT 0,
  locked_reason VARCHAR(500) NULL,
  locked_at DATETIME NULL,
  unlocked_at DATETIME NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_monthly_account_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS cutting_release_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  request_type VARCHAR(30) NOT NULL,
  requires_director_approval TINYINT NOT NULL,
  boss_verbal_approved TINYINT NOT NULL DEFAULT 0,
  boss_confirmed_at DATETIME NULL,
  request_reason VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL DEFAULT '待财务确认',
  requested_by BIGINT NOT NULL,
  requested_at DATETIME NOT NULL,
  finance_confirmed_by BIGINT NULL,
  finance_confirmed_at DATETIME NULL,
  finance_remark VARCHAR(500) NULL,
  director_approved_by BIGINT NULL,
  director_approved_at DATETIME NULL,
  director_remark VARCHAR(500) NULL,
  released_by BIGINT NULL,
  released_at DATETIME NULL,
  KEY idx_cutting_release_order (customer_order_id),
  KEY idx_cutting_release_status (status),
  CONSTRAINT fk_cutting_release_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id)
);

CREATE TABLE IF NOT EXISTS hardware_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  hardware_code VARCHAR(50) NOT NULL,
  hardware_name VARCHAR(150) NOT NULL,
  specification VARCHAR(255) NULL,
  unit VARCHAR(30) NOT NULL,
  sale_price DECIMAL(14,4) NOT NULL DEFAULT 0,
  stock_mode VARCHAR(20) NOT NULL DEFAULT 'STOCK',
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_hardware_code (hardware_code)
);

CREATE TABLE IF NOT EXISTS hardware_stock (
  hardware_item_id BIGINT PRIMARY KEY,
  on_hand_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  reserved_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  available_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  min_stock_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_hardware_stock_item FOREIGN KEY (hardware_item_id) REFERENCES hardware_item(id)
);

CREATE TABLE IF NOT EXISTS inventory_reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_order_id BIGINT NOT NULL,
  factory_order_id VARCHAR(50) NOT NULL,
  hardware_item_id BIGINT NOT NULL,
  required_quantity DECIMAL(14,4) NOT NULL,
  reserved_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  shortage_quantity DECIMAL(14,4) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT '待下料锁定',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_inventory_reservation_order (customer_order_id),
  CONSTRAINT fk_inventory_reservation_order FOREIGN KEY (customer_order_id) REFERENCES customer_order(id),
  CONSTRAINT fk_inventory_reservation_factory FOREIGN KEY (factory_order_id) REFERENCES factory_order(factory_order_id),
  CONSTRAINT fk_inventory_reservation_hardware FOREIGN KEY (hardware_item_id) REFERENCES hardware_item(id)
);

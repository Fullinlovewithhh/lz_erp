-- Remove the retired project layer from the formal customer-order flow.
-- Run this script once on an existing database after taking a full backup.
USE gc_erp;

-- Keep a read-only snapshot while legacy contract code is still being retired.
CREATE TABLE IF NOT EXISTS legacy_project_archive LIKE project;
INSERT IGNORE INTO legacy_project_archive SELECT * FROM project;

DROP PROCEDURE IF EXISTS drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL drop_index_if_exists('customer_order', 'uk_customer_order_customer_no');
CALL drop_index_if_exists('customer_order', 'uk_customer_order_no');
CALL drop_index_if_exists('customer_order', 'uk_customer_order_project_no');
CALL drop_index_if_exists('customer_order', 'idx_customer_order_project_id');
CALL drop_index_if_exists('customer_order', 'idx_customer_order_customer_no');
DROP PROCEDURE IF EXISTS drop_index_if_exists;

DROP PROCEDURE IF EXISTS drop_column_if_exists;
DELIMITER //
CREATE PROCEDURE drop_column_if_exists(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL drop_column_if_exists('customer_order', 'project_id');
DROP PROCEDURE IF EXISTS drop_column_if_exists;

ALTER TABLE customer_order
  ADD UNIQUE INDEX uk_customer_order_no (customer_order_no);

UPDATE production_line SET line_name='实木生产线',updated_at=NOW() WHERE line_code='L';
UPDATE production_line SET line_name='板式生产线',updated_at=NOW() WHERE line_code='V';

-- The project table remains only for read compatibility with the disabled legacy
-- contract flow. Drop it together with contract.project_id when that flow is
-- physically removed; new customer orders no longer read or write either field.

package com.gcerp.erp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaCompatibilityInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS project (
                  project_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  project_no VARCHAR(50) NOT NULL UNIQUE,
                  project_name VARCHAR(100) NOT NULL,
                  customer_id BIGINT NOT NULL,
                  project_address VARCHAR(255) NULL,
                  project_status VARCHAR(50) NOT NULL DEFAULT '进行中',
                  project_manager VARCHAR(100) NULL,
                  remark TEXT NULL,
                  is_deleted TINYINT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  KEY idx_project_customer_id (customer_id),
                  KEY idx_project_no (project_no),
                  KEY idx_project_name (project_name)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS customer_order (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  customer_id BIGINT NOT NULL,
                  project_id BIGINT NOT NULL,
                  customer_order_no VARCHAR(255) NOT NULL,
                  customer_order_name VARCHAR(255) NULL,
                  remark TEXT NULL,
                  status VARCHAR(50) NOT NULL DEFAULT '启用',
                  is_deleted TINYINT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  KEY idx_customer_order_customer_id (customer_id),
                  KEY idx_customer_order_project_id (project_id),
                  KEY idx_customer_order_no (customer_order_no),
                  UNIQUE KEY uk_customer_order_project_no (project_id, customer_order_no)
                )
                """);

        addColumnIfMissing("contract", "customer_id", "BIGINT NULL");
        addColumnIfMissing("contract", "project_id", "BIGINT NULL");
        addColumnIfMissing("contract", "customer_order_id", "BIGINT NULL");
        addColumnIfMissing("contract", "factory_order_no", "VARCHAR(50) NULL");
        addColumnIfMissing("contract", "customer_order_no", "VARCHAR(255) NULL");
        addColumnIfMissing("contract", "customer_name_snapshot", "VARCHAR(100) NULL");
        addColumnIfMissing("contract", "customer_phone_snapshot", "VARCHAR(30) NULL");
        addColumnIfMissing("contract", "customer_address_snapshot", "VARCHAR(255) NULL");
        addColumnIfMissing("contract", "project_name_snapshot", "VARCHAR(100) NULL");
        jdbcTemplate.update("UPDATE contract SET factory_order_no = contract_no WHERE factory_order_no IS NULL OR factory_order_no = ''");
        addIndexIfMissing("contract", "idx_contract_customer_id", "customer_id");
        addIndexIfMissing("contract", "idx_contract_project_id", "project_id");
        addIndexIfMissing("contract", "idx_contract_customer_order_id", "customer_order_id");
        addIndexIfMissing("contract", "idx_contract_factory_order_no", "factory_order_no");
        addIndexIfMissing("contract", "idx_contract_customer_order_no", "customer_order_no");
        migrateCustomerOrders();
    }

    private void migrateCustomerOrders() {
        jdbcTemplate.update("""
                INSERT INTO customer_order(customer_id, project_id, customer_order_no, customer_order_name, status, is_deleted, created_at, updated_at)
                SELECT DISTINCT c.customer_id,
                                c.project_id,
                                c.customer_order_no,
                                c.customer_order_no,
                                '启用',
                                0,
                                NOW(),
                                NOW()
                FROM contract c
                LEFT JOIN customer_order co
                  ON co.project_id = c.project_id
                 AND co.customer_order_no = c.customer_order_no
                 AND co.is_deleted = 0
                WHERE c.customer_id IS NOT NULL
                  AND c.project_id IS NOT NULL
                  AND c.customer_order_no IS NOT NULL
                  AND c.customer_order_no <> ''
                  AND co.id IS NULL
                """);
        jdbcTemplate.update("""
                UPDATE contract c
                JOIN customer_order co
                  ON co.project_id = c.project_id
                 AND co.customer_order_no = c.customer_order_no
                 AND co.is_deleted = 0
                SET c.customer_order_id = co.id
                WHERE c.customer_order_id IS NULL
                  AND c.customer_order_no IS NOT NULL
                  AND c.customer_order_no <> ''
                """);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void addIndexIfMissing(String table, String indexName, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                """, Integer.class, table, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD INDEX " + indexName + " (" + column + ")");
        }
    }
}

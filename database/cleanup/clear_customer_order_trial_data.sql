-- Clear trial data for:
-- 2.1 customer information
-- 3.1 project orders
-- Linked generated data: projects, contracts, quotes, quote details,
-- quote assignments, payments, vouchers, production tasks, workflow logs,
-- quote detail extra prices, and operation logs.
--
-- Kept data:
-- app_user, staff, product, material, price_rule, process_rule, quote_special_rule.

USE gc_erp;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE quote_detail_extra_price;
TRUNCATE TABLE quote_detail;
TRUNCATE TABLE quote_assignment;
TRUNCATE TABLE quotation;

TRUNCATE TABLE payment_record;
TRUNCATE TABLE finance_voucher;

TRUNCATE TABLE production_task;
TRUNCATE TABLE workflow_log;
TRUNCATE TABLE contract_cad_file;
TRUNCATE TABLE contract;

TRUNCATE TABLE customer_followup;
TRUNCATE TABLE customer_requirement;
TRUNCATE TABLE project;
TRUNCATE TABLE customer;

TRUNCATE TABLE operation_log;

SET FOREIGN_KEY_CHECKS = 1;


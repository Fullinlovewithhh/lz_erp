-- Clear all V2/V3 trial orders and linked transactional data.
-- Keep customers, projects, users, staff, master data, production lines,
-- receiving accounts, hardware master/stock and quotation rules.

USE gc_erp;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE inventory_reservation;
TRUNCATE TABLE factory_order_quote_item_extra_price;
TRUNCATE TABLE factory_order_quote_item;
TRUNCATE TABLE factory_order_quote;
TRUNCATE TABLE factory_order_assignment_log;
TRUNCATE TABLE customer_order_price_adjustment;
TRUNCATE TABLE customer_quote_confirmation;
TRUNCATE TABLE customer_quote_pdf;
TRUNCATE TABLE customer_payment_receipt;
TRUNCATE TABLE customer_payment_plan;
TRUNCATE TABLE cutting_release_request;
TRUNCATE TABLE customer_order_split_draft;
TRUNCATE TABLE customer_order_cad_file;
TRUNCATE TABLE factory_order;

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

TRUNCATE TABLE customer_order;
TRUNCATE TABLE operation_log;

UPDATE customer_monthly_account
SET overdue_locked=0, locked_reason=NULL, locked_at=NULL, unlocked_at=NOW(), updated_at=NOW();

SET FOREIGN_KEY_CHECKS = 1;

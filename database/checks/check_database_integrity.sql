-- ERP database integrity checks.
-- This script only reads data. It does not modify any table.
--
-- How to read the result:
-- - Each SELECT checks one risk category.
-- - Empty result means no issue was found for that category.
-- - Fix all returned rows before adding foreign keys.

USE gc_erp;

-- 1. Duplicate business numbers.
SELECT
  'DUPLICATE_FACTORY_ORDER_NO' AS issue_type,
  factory_order_no,
  COUNT(*) AS duplicate_count
FROM contract
WHERE factory_order_no IS NOT NULL AND factory_order_no <> ''
GROUP BY factory_order_no
HAVING COUNT(*) > 1;

SELECT
  'DUPLICATE_CONTRACT_NO' AS issue_type,
  contract_no,
  COUNT(*) AS duplicate_count
FROM contract
WHERE contract_no IS NOT NULL AND contract_no <> ''
GROUP BY contract_no
HAVING COUNT(*) > 1;

SELECT
  'DUPLICATE_CUSTOMER_ORDER_NO_IN_PROJECT' AS issue_type,
  project_id,
  customer_order_no,
  COUNT(*) AS duplicate_count
FROM customer_order
WHERE is_deleted = 0
  AND customer_order_no IS NOT NULL
  AND customer_order_no <> ''
GROUP BY project_id, customer_order_no
HAVING COUNT(*) > 1;

SELECT
  'DUPLICATE_PROJECT_NO' AS issue_type,
  project_no,
  COUNT(*) AS duplicate_count
FROM project
WHERE project_no IS NOT NULL AND project_no <> ''
GROUP BY project_no
HAVING COUNT(*) > 1;

SELECT
  'DUPLICATE_CUSTOMER_CODE' AS issue_type,
  customer_code,
  COUNT(*) AS duplicate_count
FROM customer
WHERE customer_code IS NOT NULL AND customer_code <> ''
GROUP BY customer_code
HAVING COUNT(*) > 1;

-- 2. Factory orders missing the formal three-level relationship.
SELECT
  'CONTRACT_MISSING_CUSTOMER_ORDER_ID' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.contract_no,
  c.customer_id,
  c.project_id,
  c.customer_order_id,
  c.customer_order_no
FROM contract c
WHERE c.customer_order_id IS NULL;

SELECT
  'CONTRACT_MISSING_PROJECT_OR_CUSTOMER' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.customer_id,
  c.project_id,
  c.customer_order_id
FROM contract c
WHERE c.customer_id IS NULL
   OR c.project_id IS NULL;

-- 3. Broken references from the main chain.
SELECT
  'PROJECT_CUSTOMER_NOT_FOUND' AS issue_type,
  p.project_id,
  p.project_no,
  p.project_name,
  p.customer_id
FROM project p
LEFT JOIN customer cu ON cu.id = p.customer_id
WHERE p.is_deleted = 0
  AND cu.id IS NULL;

SELECT
  'CUSTOMER_ORDER_CUSTOMER_NOT_FOUND' AS issue_type,
  co.id AS customer_order_id,
  co.customer_id,
  co.project_id,
  co.customer_order_no
FROM customer_order co
LEFT JOIN customer cu ON cu.id = co.customer_id
WHERE co.is_deleted = 0
  AND cu.id IS NULL;

SELECT
  'CUSTOMER_ORDER_PROJECT_NOT_FOUND' AS issue_type,
  co.id AS customer_order_id,
  co.customer_id,
  co.project_id,
  co.customer_order_no
FROM customer_order co
LEFT JOIN project p ON p.project_id = co.project_id AND p.is_deleted = 0
WHERE co.is_deleted = 0
  AND p.project_id IS NULL;

SELECT
  'CONTRACT_CUSTOMER_NOT_FOUND' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.customer_id
FROM contract c
LEFT JOIN customer cu ON cu.id = c.customer_id
WHERE c.customer_id IS NOT NULL
  AND cu.id IS NULL;

SELECT
  'CONTRACT_PROJECT_NOT_FOUND' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.project_id
FROM contract c
LEFT JOIN project p ON p.project_id = c.project_id AND p.is_deleted = 0
WHERE c.project_id IS NOT NULL
  AND p.project_id IS NULL;

SELECT
  'CONTRACT_CUSTOMER_ORDER_NOT_FOUND' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.customer_order_id
FROM contract c
LEFT JOIN customer_order co ON co.id = c.customer_order_id AND co.is_deleted = 0
WHERE c.customer_order_id IS NOT NULL
  AND co.id IS NULL;

-- 4. Parent-child ownership mismatch in the three-level structure.
SELECT
  'PROJECT_CUSTOMER_MISMATCH' AS issue_type,
  p.project_id,
  p.project_no,
  p.customer_id AS project_customer_id,
  co.id AS customer_order_id,
  co.customer_id AS customer_order_customer_id,
  co.customer_order_no
FROM customer_order co
JOIN project p ON p.project_id = co.project_id
WHERE co.is_deleted = 0
  AND p.is_deleted = 0
  AND co.customer_id <> p.customer_id;

SELECT
  'CONTRACT_CUSTOMER_ORDER_MISMATCH' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.customer_id AS contract_customer_id,
  c.project_id AS contract_project_id,
  co.id AS customer_order_id,
  co.customer_id AS customer_order_customer_id,
  co.project_id AS customer_order_project_id,
  co.customer_order_no
FROM contract c
JOIN customer_order co ON co.id = c.customer_order_id
WHERE co.is_deleted = 0
  AND (
    c.customer_id <> co.customer_id
    OR c.project_id <> co.project_id
  );

SELECT
  'CONTRACT_CUSTOMER_ORDER_NO_SNAPSHOT_MISMATCH' AS issue_type,
  c.id AS contract_id,
  c.factory_order_no,
  c.customer_order_id,
  c.customer_order_no AS contract_customer_order_no,
  co.customer_order_no AS formal_customer_order_no
FROM contract c
JOIN customer_order co ON co.id = c.customer_order_id
WHERE co.is_deleted = 0
  AND c.customer_order_no IS NOT NULL
  AND c.customer_order_no <> ''
  AND c.customer_order_no <> co.customer_order_no;

-- 5. Business child rows pointing to missing factory orders.
SELECT
  'QUOTE_ASSIGNMENT_CONTRACT_NOT_FOUND' AS issue_type,
  qa.id AS assignment_id,
  qa.contract_id,
  qa.contract_no
FROM quote_assignment qa
LEFT JOIN contract c ON c.id = qa.contract_id
WHERE c.id IS NULL;

SELECT
  'QUOTATION_CONTRACT_NOT_FOUND' AS issue_type,
  q.id AS quote_id,
  q.contract_id,
  q.assignment_id
FROM quotation q
LEFT JOIN contract c ON c.id = q.contract_id
WHERE c.id IS NULL;

SELECT
  'QUOTATION_ASSIGNMENT_NOT_FOUND' AS issue_type,
  q.id AS quote_id,
  q.contract_id,
  q.assignment_id
FROM quotation q
LEFT JOIN quote_assignment qa ON qa.id = q.assignment_id
WHERE q.assignment_id IS NOT NULL
  AND qa.id IS NULL;

SELECT
  'QUOTE_DETAIL_QUOTATION_NOT_FOUND' AS issue_type,
  d.id AS quote_detail_id,
  d.quote_id
FROM quote_detail d
LEFT JOIN quotation q ON q.id = d.quote_id
WHERE d.quote_id IS NOT NULL
  AND q.id IS NULL;

SELECT
  'QUOTE_EXTRA_DETAIL_NOT_FOUND' AS issue_type,
  ep.id AS extra_price_id,
  ep.detail_id,
  ep.quote_id
FROM quote_detail_extra_price ep
LEFT JOIN quote_detail d ON d.id = ep.detail_id
WHERE d.id IS NULL;

SELECT
  'QUOTE_EXTRA_QUOTATION_NOT_FOUND' AS issue_type,
  ep.id AS extra_price_id,
  ep.detail_id,
  ep.quote_id
FROM quote_detail_extra_price ep
LEFT JOIN quotation q ON q.id = ep.quote_id
WHERE q.id IS NULL;

SELECT
  'PAYMENT_CONTRACT_NOT_FOUND' AS issue_type,
  pr.id AS payment_record_id,
  pr.contract_id
FROM payment_record pr
LEFT JOIN contract c ON c.id = pr.contract_id
WHERE c.id IS NULL;

SELECT
  'PRODUCTION_CONTRACT_NOT_FOUND' AS issue_type,
  pt.id AS production_task_id,
  pt.contract_id,
  pt.task_no
FROM production_task pt
LEFT JOIN contract c ON c.id = pt.contract_id
WHERE c.id IS NULL;

SELECT
  'CAD_FILE_CONTRACT_NOT_FOUND' AS issue_type,
  cf.id AS cad_file_id,
  cf.contract_id,
  cf.contract_no
FROM contract_cad_file cf
LEFT JOIN contract c ON c.id = cf.contract_id
WHERE c.id IS NULL;

SELECT
  'FINANCE_VOUCHER_CONTRACT_NOT_FOUND' AS issue_type,
  fv.id AS finance_voucher_id,
  fv.contract_id,
  fv.contract_no
FROM finance_voucher fv
LEFT JOIN contract c ON c.id = fv.contract_id
WHERE c.id IS NULL;

SELECT
  'WORKFLOW_LOG_CONTRACT_NOT_FOUND' AS issue_type,
  wl.id AS workflow_log_id,
  wl.contract_id
FROM workflow_log wl
LEFT JOIN contract c ON c.id = wl.contract_id
WHERE c.id IS NULL;

-- 6. Legacy compatibility fields that should stay aligned.
SELECT
  'CONTRACT_NO_FACTORY_ORDER_NO_MISMATCH' AS issue_type,
  c.id AS contract_id,
  c.contract_no,
  c.factory_order_no
FROM contract c
WHERE c.contract_no IS NOT NULL
  AND c.factory_order_no IS NOT NULL
  AND c.contract_no <> c.factory_order_no;

SELECT
  'FACTORY_ORDER_NO_MISSING' AS issue_type,
  c.id AS contract_id,
  c.contract_no,
  c.factory_order_no
FROM contract c
WHERE c.factory_order_no IS NULL
   OR c.factory_order_no = '';

USE gc_erp;

-- Every SELECT below should return zero rows.
SELECT 'INVALID_FACTORY_ORDER_ID' issue, factory_order_id reference_value
FROM factory_order
WHERE factory_order_id NOT REGEXP '^(BD)?[A-Z0-9]{1,10}[0-9]{8}[0-9]{3}$';

SELECT 'ORPHAN_FACTORY_ORDER' issue, fo.factory_order_id reference_value
FROM factory_order fo
LEFT JOIN customer_order co ON co.id=fo.customer_order_id
WHERE co.id IS NULL;

SELECT 'INVALID_SUPPLEMENT_PARENT' issue, fo.factory_order_id reference_value
FROM factory_order fo
LEFT JOIN factory_order parent ON parent.factory_order_id=fo.parent_factory_order_id
WHERE fo.order_type='SUPPLEMENT' AND parent.factory_order_id IS NULL;

SELECT 'HARDWARE_DISCOUNT_ENABLED' issue, CAST(qi.id AS CHAR) reference_value
FROM factory_order_quote_item qi
WHERE qi.product_category IN ('HARDWARE','五金配件') AND qi.discount_eligible<>0;

SELECT 'ORPHAN_QUOTE' issue, CAST(q.id AS CHAR) reference_value
FROM factory_order_quote q
LEFT JOIN factory_order fo ON fo.factory_order_id=q.factory_order_id
WHERE fo.factory_order_id IS NULL;

SELECT 'ORPHAN_PAYMENT_PLAN' issue, CAST(pp.id AS CHAR) reference_value
FROM customer_payment_plan pp
LEFT JOIN customer_order co ON co.id=pp.customer_order_id
WHERE co.id IS NULL;

SELECT 'ORPHAN_RECEIPT' issue, CAST(r.id AS CHAR) reference_value
FROM customer_payment_receipt r
LEFT JOIN customer_order co ON co.id=r.customer_order_id
WHERE co.id IS NULL;

SELECT 'NEGATIVE_COMMERCIAL_AMOUNT' issue, CAST(co.id AS CHAR) reference_value
FROM customer_order co
WHERE co.quote_total_amount<0 OR co.final_receivable_amount<0;

SELECT 'RELEASE_WITHOUT_REQUIRED_APPROVAL' issue, CAST(cr.id AS CHAR) reference_value
FROM cutting_release_request cr
WHERE cr.status='已放行'
  AND (cr.finance_confirmed_at IS NULL
       OR (cr.requires_director_approval=1 AND cr.director_approved_at IS NULL));

SELECT 'NEGATIVE_HARDWARE_AVAILABLE' issue, CAST(hs.hardware_item_id AS CHAR) reference_value
FROM hardware_stock hs
WHERE hs.on_hand_quantity<0 OR hs.reserved_quantity<0 OR hs.available_quantity<0;

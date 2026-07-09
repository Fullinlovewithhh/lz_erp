# Database Integrity Check Report

Run date: 2026-07-09

## Scope

Checked the current `gc_erp` database before adding stricter foreign key constraints.

Script:

```text
database/checks/check_database_integrity.sql
```

Result file:

```text
database/checks/check_database_integrity_result.txt
```

## Current Row Counts

| Table | Rows |
| --- | ---: |
| `customer` | 1 |
| `project` | 1 |
| `customer_order` | 1 |
| `contract` | 1 |
| `quote_assignment` | 1 |
| `quotation` | 2 |
| `quote_detail` | 2 |
| `payment_record` | 0 |
| `production_task` | 0 |

## Result

No integrity issues were returned by the health check script.

The current data passes the pre-check for:

- duplicate factory order numbers
- duplicate customer order numbers within the same project
- missing `customer_order_id` on factory orders
- broken customer, project, and customer order references
- mismatch between project, customer order, and factory order ownership
- quote, payment, production, CAD, finance voucher, and workflow rows pointing to missing factory orders
- mismatch between `contract_no` and `factory_order_no`

## Next Step

The database is ready for the next strict-architecture step:

```text
Tighten the 3.1 edit order entry so customer order numbers are selected by customer_order_id instead of manually typed.
```

Foreign keys should still be added after the UI/import entry points are tightened, so new dirty data cannot be introduced after the constraints are designed.

-- Backfill rollback helper for the orders service.
--
-- 1. Populate the temporary table with the order numbers that need to be reverted.
--    Example:
--      INSERT INTO rollback_order_numbers VALUES ('ORD-1001');
-- 2. Replace <AUDIT_ID> with the value recorded in orders.backfill_audit.
-- 3. Execute the script within a transaction to safely undo the migration.
--
-- Note: temporary tables exist only for the current session and will be dropped automatically.

BEGIN;

CREATE TEMP TABLE rollback_order_numbers (order_number TEXT PRIMARY KEY);

-- TODO: insert the list of affected order numbers before running the statements below.
-- INSERT INTO rollback_order_numbers(order_number) VALUES ('ORD-EXAMPLE-123');

DELETE FROM orders.orders o
WHERE EXISTS (
    SELECT 1 FROM rollback_order_numbers r WHERE r.order_number = o.order_number
);

DELETE FROM orders.backfill_audit
WHERE id = <AUDIT_ID>;

DROP TABLE rollback_order_numbers;

COMMIT;

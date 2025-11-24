-- Add idempotency_key column to transactions table
ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(255);

-- Add unique index on idempotency_key
CREATE UNIQUE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);

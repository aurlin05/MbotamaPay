-- Add version column to transactions table for optimistic locking
ALTER TABLE transactions ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Update existing rows to have version 0
UPDATE transactions SET version = 0 WHERE version IS NULL;

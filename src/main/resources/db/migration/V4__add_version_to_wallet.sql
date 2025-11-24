-- Add version column to wallets table for optimistic locking
ALTER TABLE wallets ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Update existing rows to have version 0
UPDATE wallets SET version = 0 WHERE version IS NULL;

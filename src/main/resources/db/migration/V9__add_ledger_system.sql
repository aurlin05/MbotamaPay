-- Migration V9: Add Ledger System, Reserved Balance, and Refresh Tokens
-- This migration implements double-entry accounting and refresh token support

-- Add reserved_balance to wallets
ALTER TABLE wallets ADD COLUMN reserved_balance DECIMAL(19,2) NOT NULL DEFAULT 0.00;

-- Create ledger_entries table for double-entry accounting
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    reference_id VARCHAR(255) NOT NULL,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    entry_type VARCHAR(20) NOT NULL, -- DEBIT or CREDIT
    transaction_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    metadata TEXT, -- JSON stored as text
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for ledger_entries
CREATE INDEX idx_ledger_entries_reference_id ON ledger_entries(reference_id);
CREATE INDEX idx_ledger_entries_wallet_id ON ledger_entries(wallet_id);
CREATE INDEX idx_ledger_entries_created_at ON ledger_entries(created_at);
CREATE INDEX idx_ledger_entries_wallet_created ON ledger_entries(wallet_id, created_at DESC);

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for refresh_tokens
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Add comment for documentation
COMMENT ON TABLE ledger_entries IS 'Double-entry accounting ledger for all wallet transactions';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for extended authentication sessions';
COMMENT ON COLUMN wallets.reserved_balance IS 'Amount reserved for pending payouts/operations';

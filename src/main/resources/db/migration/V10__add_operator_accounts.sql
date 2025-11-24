-- Migration V10: Add Operator Accounts for Provider Liquidity Management
-- This migration creates tables for managing FeexPay and CinetPay operator balances

-- Create operator_accounts table
CREATE TABLE IF NOT EXISTS operator_accounts (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL, -- FEEXPAY or CINETPAY
    currency VARCHAR(10) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    reserved_balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_provider_currency UNIQUE (provider, currency)
);

-- Create index for operator_accounts
CREATE INDEX idx_operator_accounts_provider ON operator_accounts(provider);
CREATE INDEX idx_operator_accounts_currency ON operator_accounts(currency);

-- Add comment for documentation
COMMENT ON TABLE operator_accounts IS 'Tracks merchant balances on FeexPay and CinetPay for liquidity management';
COMMENT ON COLUMN operator_accounts.reserved_balance IS 'Amount reserved for pending payouts';
COMMENT ON COLUMN operator_accounts.last_sync_at IS 'Last time balance was synced with provider API';

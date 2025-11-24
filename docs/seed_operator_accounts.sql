-- Seed Data for Operator Accounts
-- This script initializes operator accounts with starting balances for FeexPay and CinetPay
-- Run this after migrations to enable bridge and liquidity management features

-- Insert operator accounts for FeexPay
INSERT INTO operator_accounts (provider, currency, balance, reserved_balance, created_at, updated_at)
VALUES 
    ('FEEXPAY', 'XAF', 1000000.00, 0.00, NOW(), NOW()),
    ('FEEXPAY', 'XOF', 500000.00, 0.00, NOW(), NOW()),
    ('FEEXPAY', 'CDF', 300000.00, 0.00, NOW(), NOW())
ON CONFLICT (provider, currency) DO NOTHING;

-- Insert operator accounts for CinetPay
INSERT INTO operator_accounts (provider, currency, balance, reserved_balance, created_at, updated_at)
VALUES 
    ('CINETPAY', 'XOF', 800000.00, 0.00, NOW(), NOW()),
    ('CINETPAY', 'XAF', 300000.00, 0.00, NOW(), NOW()),
    ('CINETPAY', 'CDF', 200000.00, 0.00, NOW(), NOW())
ON CONFLICT (provider, currency) DO NOTHING;

-- Verify inserted data
SELECT 
    provider,
    currency,
    balance,
    reserved_balance,
    balance - reserved_balance AS available_balance,
    created_at
FROM operator_accounts
ORDER BY provider, currency;

-- Summary
SELECT 
    provider,
    COUNT(*) AS account_count,
    SUM(balance) AS total_balance,
    SUM(reserved_balance) AS total_reserved,
    SUM(balance - reserved_balance) AS total_available
FROM operator_accounts
GROUP BY provider;

-- Migration V11: Add Fee Rules and Exchange Rates
-- This migration creates tables for configurable fees and currency exchange rates

-- Create exchange_rates table
CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    rate DECIMAL(19,6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_currency_pair UNIQUE (from_currency, to_currency)
);

-- Create fee_rules table
CREATE TABLE IF NOT EXISTS fee_rules (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL, -- FEEXPAY, CINETPAY, or INTERNAL
    transaction_type VARCHAR(50) NOT NULL,
    percentage_fee DECIMAL(5,2) NOT NULL DEFAULT 0.00, -- e.g., 1.50 for 1.5%
    fixed_fee DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_exchange_rates_from_currency ON exchange_rates(from_currency);
CREATE INDEX idx_exchange_rates_to_currency ON exchange_rates(to_currency);
CREATE INDEX idx_fee_rules_provider_type ON fee_rules(provider, transaction_type);
CREATE INDEX idx_fee_rules_active ON fee_rules(active);

-- Insert default exchange rates (1:1 for same currency)
INSERT INTO exchange_rates (from_currency, to_currency, rate) VALUES
    ('XAF', 'XAF', 1.000000),
    ('XOF', 'XOF', 1.000000),
    ('CDF', 'CDF', 1.000000),
    ('XAF', 'XOF', 1.000000), -- XAF and XOF have same value
    ('XOF', 'XAF', 1.000000);

-- Insert default fee rules (1.5% + 100 minor currency units)
INSERT INTO fee_rules (provider, transaction_type, percentage_fee, fixed_fee, currency, active) VALUES
    ('FEEXPAY', 'TOP_UP', 1.50, 100.00, 'XAF', true),
    ('FEEXPAY', 'WITHDRAW', 1.50, 100.00, 'XAF', true),
    ('CINETPAY', 'TOP_UP', 1.50, 100.00, 'XOF', true),
    ('CINETPAY', 'WITHDRAW', 1.50, 100.00, 'XOF', true),
    ('INTERNAL', 'P2P_TRANSFER', 0.50, 50.00, 'XAF', true);

-- Add comments for documentation
COMMENT ON TABLE exchange_rates IS 'Currency exchange rates for multi-currency support';
COMMENT ON TABLE fee_rules IS 'Configurable fee rules per provider and transaction type';
COMMENT ON COLUMN fee_rules.percentage_fee IS 'Percentage fee (e.g., 1.50 = 1.5%)';
COMMENT ON COLUMN fee_rules.fixed_fee IS 'Fixed fee in minor currency units';

-- Migration V12: Add Webhook Logs for Audit Trail
-- This migration creates table for storing webhook payloads from payment providers

-- Create webhook_logs table
CREATE TABLE IF NOT EXISTS webhook_logs (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL, -- FEEXPAY or CINETPAY
    event_type VARCHAR(100) NOT NULL, -- e.g., PAYMENT_SUCCESS, PAYOUT_COMPLETED
    payload TEXT NOT NULL, -- JSON stored as text
    signature VARCHAR(500), -- HMAC signature for verification
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED, PROCESSED, FAILED
    error_message TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for webhook_logs
CREATE INDEX idx_webhook_logs_provider ON webhook_logs(provider);
CREATE INDEX idx_webhook_logs_created_at ON webhook_logs(created_at DESC);
CREATE INDEX idx_webhook_logs_status ON webhook_logs(status);
CREATE INDEX idx_webhook_logs_provider_created ON webhook_logs(provider, created_at DESC);

-- Add comment for documentation
COMMENT ON TABLE webhook_logs IS 'Audit trail for all webhook events from payment providers';
COMMENT ON COLUMN webhook_logs.payload IS 'Raw JSON payload from provider webhook';
COMMENT ON COLUMN webhook_logs.signature IS 'HMAC signature for webhook verification';

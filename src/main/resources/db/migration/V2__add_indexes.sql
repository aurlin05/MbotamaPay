-- Add indexes for better query performance

-- User indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);

-- Transaction indexes
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_sender_wallet ON transactions(sender_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_receiver_wallet ON transactions(receiver_wallet_id);
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON transactions(reference);

-- Wallet indexes
CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets(user_id);

-- Notification indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);

-- Payment Request indexes
CREATE INDEX IF NOT EXISTS idx_payment_requests_requester ON payment_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_payment_requests_payer ON payment_requests(payer_id);
CREATE INDEX IF NOT EXISTS idx_payment_requests_status ON payment_requests(status);
CREATE INDEX IF NOT EXISTS idx_payment_requests_created_at ON payment_requests(created_at DESC);

-- OTP Verification indexes
CREATE INDEX IF NOT EXISTS idx_otp_recipient ON otp_verifications(recipient);
CREATE INDEX IF NOT EXISTS idx_otp_expiry ON otp_verifications(expiry_time);

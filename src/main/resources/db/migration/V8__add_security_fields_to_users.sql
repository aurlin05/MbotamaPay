-- Add security tracking fields to users table

-- Add failed login attempts counter
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;

-- Add account lock timestamp
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;

-- Add referral code if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='referral_code') THEN
        ALTER TABLE users ADD COLUMN referral_code VARCHAR(50) UNIQUE;
    END IF;
END $$;

-- Add referrer_id if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='users' AND column_name='referrer_id') THEN
        ALTER TABLE users ADD COLUMN referrer_id BIGINT REFERENCES users(id);
    END IF;
END $$;

-- Create index on locked_until for efficient queries
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until) WHERE locked_until IS NOT NULL;

-- Insert admin user for testing
-- Password: admin123 (BCrypt hash)
-- Email: admin@mbotamapay.com

INSERT INTO users (
    name,
    email,
    phone,
    password_hash,
    role,
    kyc_level,
    active,
    referral_code,
    failed_login_attempts,
    locked_until,
    created_at,
    updated_at
) VALUES (
    'Admin User',
    'admin@mbotamapay.com',
    '+237600000000',
    '$2a$10$8K1p/a0dL3.ksOJ7P9p.Iu.INA/a7VQVN0nOQYheBNNSWn81sA8tVW',
    'ADMIN',
    'LEVEL_2',
    true,
    'ADMIN2024',
    0,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- Create wallet for admin user
INSERT INTO wallets (
    user_id,
    balance,
    currency,
    created_at,
    updated_at
)
SELECT
    id,
    0.00,
    'XAF',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users
WHERE email = 'admin@mbotamapay.com'
ON CONFLICT (user_id) DO NOTHING;

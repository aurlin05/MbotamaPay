-- Add new columns to audit_logs table
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS action_type VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS severity VARCHAR(50) NOT NULL DEFAULT 'INFO';
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS ip_address VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

-- Update action_type from existing action column
UPDATE audit_logs SET action_type = action WHERE action_type IS NULL;

-- Update created_at from existing timestamp column
UPDATE audit_logs SET created_at = timestamp WHERE created_at IS NULL;

-- Make action_type NOT NULL after data migration
ALTER TABLE audit_logs ALTER COLUMN action_type SET NOT NULL;

-- Make created_at NOT NULL after data migration
ALTER TABLE audit_logs ALTER COLUMN created_at SET NOT NULL;

-- Add foreign key constraint for user_id
ALTER TABLE audit_logs ADD CONSTRAINT fk_audit_log_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_action_type ON audit_logs(action_type);

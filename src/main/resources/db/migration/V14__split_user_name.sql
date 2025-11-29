-- Split user name into first_name and last_name
-- This migration splits the existing 'name' column into separate first_name and last_name columns

-- Add new columns
ALTER TABLE users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN last_name VARCHAR(255);

-- Migrate existing data
-- Split name on first space: everything before = first_name, everything after = last_name
UPDATE users 
SET 
    first_name = CASE 
        WHEN position(' ' in name) > 0 THEN substring(name, 1, position(' ' in name) - 1)
        ELSE name
    END,
    last_name = CASE 
        WHEN position(' ' in name) > 0 THEN substring(name, position(' ' in name) + 1)
        ELSE ''
    END;

-- Make columns NOT NULL after migration
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;

-- Drop old name column
ALTER TABLE users DROP COLUMN name;

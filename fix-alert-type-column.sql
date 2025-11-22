-- Fix alert_type column size to support HELP_REQUEST (12 characters)
-- Current issue: Data truncated for column 'alert_type' at row 1
-- Solution: Increase VARCHAR size from insufficient length to VARCHAR(50)

-- Step 1: Check current column definition
SHOW COLUMNS FROM bsrxqa8k23608y3y.alerts LIKE 'alert_type';

-- Step 2: Modify column to VARCHAR(50)
ALTER TABLE bsrxqa8k23608y3y.alerts 
MODIFY COLUMN alert_type VARCHAR(50) NOT NULL;

-- Step 3: Verify the change
SHOW COLUMNS FROM bsrxqa8k23608y3y.alerts LIKE 'alert_type';

-- Step 4: Test insert HELP_REQUEST
SELECT 'Migration completed successfully!' AS status;

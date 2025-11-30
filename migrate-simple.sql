-- =====================================================
-- SIMPLE SQL Migration: Add employee_id column to helmets
-- Run this on JawsDB before deploying
-- =====================================================

-- Step 1: Add 'id' column to employees if not exists (auto-increment)
ALTER TABLE employees 
ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- Step 2: Add 'location' column to employees if not exists
ALTER TABLE employees 
ADD COLUMN location VARCHAR(255) DEFAULT NULL;

-- Step 3: Add employee_id column to helmets table
ALTER TABLE helmets 
ADD COLUMN employee_id BIGINT DEFAULT NULL;

-- Step 4: Drop old worker_id column from helmets (if exists)
ALTER TABLE helmets DROP COLUMN IF EXISTS worker_id;

-- Step 5: Drop workers table
DROP TABLE IF EXISTS workers;

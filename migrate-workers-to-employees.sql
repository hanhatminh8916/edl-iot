-- =====================================================
-- SQL Migration: workers → employees
-- Date: 2025-11-30
-- Purpose: Remove workers table, use employees table only
-- =====================================================

-- Step 1: Add 'id' column to employees (auto-increment primary key)
-- Note: This may fail if id column already exists
ALTER TABLE employees 
ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- Step 2: Add 'location' column to employees if not exists
ALTER TABLE employees 
ADD COLUMN location VARCHAR(255) DEFAULT NULL;

-- Step 3: Migrate data from workers to employees (if needed)
-- First, check if workers table exists and has data
INSERT INTO employees (employee_id, name, position, department, phone_number, email, location, status, created_at, updated_at)
SELECT 
    w.employee_id,
    w.full_name,
    w.position,
    w.department,
    w.phone_number,
    w.email,
    w.location,
    CASE 
        WHEN w.status = 'ACTIVE' THEN 'ACTIVE'
        WHEN w.status = 'INACTIVE' THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END,
    w.created_at,
    w.updated_at
FROM workers w
WHERE NOT EXISTS (
    SELECT 1 FROM employees e WHERE e.employee_id = w.employee_id
);

-- Step 4: Add employee_id column to helmets table (references employees.id)
ALTER TABLE helmets 
ADD COLUMN employee_id BIGINT DEFAULT NULL;

-- Step 5: Migrate helmet-worker relationships to helmet-employee
-- Link helmets to employees based on MAC address
UPDATE helmets h
JOIN employees e ON h.mac_address = e.mac_address
SET h.employee_id = e.id
WHERE h.mac_address IS NOT NULL AND e.mac_address IS NOT NULL;

-- Or link based on worker_id if workers table still exists
UPDATE helmets h
JOIN workers w ON h.worker_id = w.id
JOIN employees e ON w.employee_id = e.employee_id
SET h.employee_id = e.id
WHERE h.worker_id IS NOT NULL;

-- Step 6: Add foreign key constraint
ALTER TABLE helmets
ADD CONSTRAINT fk_helmets_employee
FOREIGN KEY (employee_id) REFERENCES employees(id)
ON DELETE SET NULL;

-- Step 7: Drop old worker_id column from helmets
ALTER TABLE helmets DROP COLUMN worker_id;

-- Step 8: Drop workers table
DROP TABLE IF EXISTS workers;

-- =====================================================
-- Verify migration
-- =====================================================
SELECT 'Employees count:' AS info, COUNT(*) AS count FROM employees;
SELECT 'Helmets with employee:' AS info, COUNT(*) AS count FROM helmets WHERE employee_id IS NOT NULL;

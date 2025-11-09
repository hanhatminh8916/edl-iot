-- Drop entire safe_zones table and recreate without UNIQUE constraint

DROP TABLE IF EXISTS safe_zones;

-- Recreate table without unique constraint
CREATE TABLE safe_zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_name VARCHAR(255) NOT NULL,
    polygon_coordinates TEXT NOT NULL,
    color VARCHAR(50) DEFAULT '#3388ff',
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(100) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verify table structure
SHOW CREATE TABLE safe_zones;

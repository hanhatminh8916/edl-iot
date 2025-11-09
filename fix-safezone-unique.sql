-- Fix SafeZone unique constraint issue
-- Chạy script này trên JawsDB để xóa UNIQUE constraint

-- Cách 1: Drop và tạo lại bảng (CẢNH BÁO: Mất hết dữ liệu)
DROP TABLE IF EXISTS safe_zones;

CREATE TABLE safe_zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_name VARCHAR(100) NOT NULL,
    polygon_coordinates TEXT NOT NULL,
    color VARCHAR(50) NOT NULL DEFAULT '#3388ff',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(100),
    INDEX idx_active (is_active),
    INDEX idx_updated (updated_at)
);

-- Cách 2: Chỉ drop UNIQUE constraint (giữ nguyên dữ liệu - nếu có)
-- ALTER TABLE safe_zones DROP INDEX zone_name;

-- Tạo bảng employees để quản lý nhân viên và mapping với MAC address
CREATE TABLE IF NOT EXISTS employees (
    employee_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    position VARCHAR(100),
    department VARCHAR(100),
    mac_address VARCHAR(20) UNIQUE,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 🚀 Index cho employee lookup by MAC
CREATE INDEX IF NOT EXISTS idx_employees_mac ON employees(mac_address);

-- Tạo bảng helmet_data để lưu dữ liệu từ các helmet
CREATE TABLE IF NOT EXISTS helmet_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mac VARCHAR(20) NOT NULL,
    voltage DOUBLE,
    current DOUBLE,
    power DOUBLE,
    battery DOUBLE,
    lat DOUBLE,
    lon DOUBLE,
    counter INT,
    employee_id VARCHAR(50),
    employee_name VARCHAR(255),
    timestamp TIMESTAMP,
    INDEX idx_mac (mac),
    INDEX idx_employee_id (employee_id),
    INDEX idx_timestamp (timestamp)
);

-- Tạo bảng messenger_users để quản lý người dùng Messenger
CREATE TABLE IF NOT EXISTS messenger_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    psid VARCHAR(255) UNIQUE NOT NULL,
    employee_id VARCHAR(50),
    subscribed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL
);

-- 🚀 TỐI ƯU: Indexes cho bảng alerts (nếu tồn tại)
-- Composite index cho upsert theo helmet + alert_type  
-- CREATE INDEX IF NOT EXISTS idx_alerts_helmet_type ON alerts(helmet_id, alert_type);
-- Index cho query by triggered_at (dashboard stats)
-- CREATE INDEX IF NOT EXISTS idx_alerts_triggered_at ON alerts(triggered_at);
-- Index cho query by status
-- CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts(status);

-- Insert dữ liệu mẫu cho testing
INSERT INTO employees (employee_id, name, position, department, mac_address, phone_number, email, status) 
VALUES 
    ('NV001', 'Nguyễn Văn An', 'Công nhân', 'Sản xuất', 'A48D004AEC24', '0901234567', 'an.nv@company.com', 'ACTIVE'),
    ('NV002', 'Trần Thị Bình', 'Kỹ sư', 'Kỹ thuật', NULL, '0902345678', 'binh.tt@company.com', 'ACTIVE'),
    ('NV003', 'Lê Văn Cường', 'Trưởng ca', 'Sản xuất', NULL, '0903456789', 'cuong.lv@company.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

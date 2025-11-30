package com.hatrustsoft.bfe_foraiot.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.config.DataInitializer;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final HelmetDataRepository helmetDataRepository;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired(required = false) // Optional: Có thể null nếu DataInitializer bị disable
    private DataInitializer dataInitializer;

    /**
     * 🔄 Migrate employees table: Add auto-increment ID column
     */
    @GetMapping("/migrate-employees")
    public ResponseEntity<Map<String, Object>> migrateEmployees() {
        List<String> logs = new ArrayList<>();
        try {
            // Step 1: Check current table structure
            logs.add("Checking employees table structure...");
            
            try {
                List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SHOW COLUMNS FROM employees"
                );
                logs.add("Current columns: " + columns.toString());
                
                boolean hasIdColumn = columns.stream()
                    .anyMatch(col -> "id".equalsIgnoreCase((String) col.get("Field")));
                
                if (hasIdColumn) {
                    logs.add("Column 'id' already exists. Checking if it's auto_increment...");
                    boolean isAutoIncrement = columns.stream()
                        .filter(col -> "id".equalsIgnoreCase((String) col.get("Field")))
                        .anyMatch(col -> {
                            String extra = (String) col.get("Extra");
                            return extra != null && extra.toLowerCase().contains("auto_increment");
                        });
                    
                    if (isAutoIncrement) {
                        return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Table already migrated correctly",
                            "logs", logs
                        ));
                    }
                }
            } catch (Exception e) {
                logs.add("Error checking table: " + e.getMessage());
            }
            
            // Step 2: Drop and recreate table with correct structure
            logs.add("Recreating employees table with correct structure...");
            
            // Backup existing data if any
            List<Map<String, Object>> existingData = new ArrayList<>();
            try {
                existingData = jdbcTemplate.queryForList("SELECT * FROM employees");
                logs.add("Backed up " + existingData.size() + " existing employees");
            } catch (Exception e) {
                logs.add("No existing data or error: " + e.getMessage());
            }
            
            // Drop foreign key constraints first
            try {
                jdbcTemplate.execute("ALTER TABLE helmets DROP FOREIGN KEY IF EXISTS FK_helmets_employee");
            } catch (Exception e) {
                logs.add("No FK to drop: " + e.getMessage());
            }
            
            // Drop the table
            try {
                jdbcTemplate.execute("DROP TABLE IF EXISTS employees");
                logs.add("Dropped old employees table");
            } catch (Exception e) {
                logs.add("Error dropping table: " + e.getMessage());
            }
            
            // Create new table with correct structure
            String createSql = """
                CREATE TABLE employees (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    employee_id VARCHAR(50) UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    position VARCHAR(100),
                    department VARCHAR(100),
                    location VARCHAR(100),
                    mac_address VARCHAR(20) UNIQUE,
                    phone_number VARCHAR(20),
                    email VARCHAR(255),
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;
            jdbcTemplate.execute(createSql);
            logs.add("Created new employees table with auto-increment ID");
            
            // Insert sample data
            String[] insertSqls = {
                "INSERT INTO employees (employee_id, name, position, department, location, mac_address, status) VALUES ('REV01', 'Nguyễn Văn A', 'Kỹ sư', 'Phòng Kỹ thuật', 'Khu Đông', 'A48D004AEC24', 'ACTIVE')",
                "INSERT INTO employees (employee_id, name, position, department, location, mac_address, status) VALUES ('REV02', 'Trần Thị B', 'Kỹ thuật viên', 'Phòng Kỹ thuật', 'Khu Bắc', 'B59E115BFD35', 'ACTIVE')",
                "INSERT INTO employees (employee_id, name, position, department, location, mac_address, status) VALUES ('REV03', 'Lê Văn C', 'Công nhân', 'Phòng Sản xuất', 'Khu Tây', 'C60F226CDE46', 'ACTIVE')"
            };
            
            for (String sql : insertSqls) {
                try {
                    jdbcTemplate.execute(sql);
                } catch (Exception e) {
                    logs.add("Insert error (ignored): " + e.getMessage());
                }
            }
            logs.add("Inserted sample employees");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Migration completed successfully!",
                "logs", logs
            ));
            
        } catch (Exception e) {
            logs.add("FATAL ERROR: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "logs", logs
            ));
        }
    }

    @PostMapping("/reset-data")
    public ResponseEntity<String> resetData() {
        try {
            // Kiểm tra xem DataInitializer có available không
            if (dataInitializer == null) {
                return ResponseEntity.badRequest()
                    .body("Tính năng reset-data đã bị tắt trên Heroku để tiết kiệm database queries. " +
                          "Vui lòng tạo dữ liệu thủ công qua API.");
            }
            
            // Delete all data
            helmetDataRepository.deleteAll();
            alertRepository.deleteAll();
            helmetRepository.deleteAll();
            employeeRepository.deleteAll();
            
            // Re-initialize sample data
            dataInitializer.run();
            
            return ResponseEntity.ok("Dữ liệu đã được reset thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Lỗi khi reset dữ liệu: " + e.getMessage());
        }
    }
}

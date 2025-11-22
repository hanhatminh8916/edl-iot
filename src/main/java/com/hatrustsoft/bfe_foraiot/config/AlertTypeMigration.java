package com.hatrustsoft.bfe_foraiot.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database migration để fix alert_type column size
 * Vấn đề: Column alert_type quá nhỏ, không chứa được 'HELP_REQUEST' (12 chars)
 * Giải pháp: ALTER TABLE để tăng size lên VARCHAR(50)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertTypeMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateAlertTypeColumn() {
        try {
            log.warn("🔧 Checking alert_type column size...");
            
            // Check current column definition
            String checkSql = "SHOW COLUMNS FROM alerts LIKE 'alert_type'";
            var columnInfo = jdbcTemplate.queryForMap(checkSql);
            String currentType = (String) columnInfo.get("Type");
            log.info("📊 Current alert_type column type: {}", currentType);
            
            // If column is too small, migrate
            if (!currentType.contains("varchar(50)") && !currentType.contains("VARCHAR(50)")) {
                log.warn("⚠️ alert_type column is too small! Migrating to VARCHAR(50)...");
                
                String migrateSql = "ALTER TABLE alerts MODIFY COLUMN alert_type VARCHAR(50) NOT NULL";
                jdbcTemplate.execute(migrateSql);
                
                log.info("✅ alert_type column migrated successfully to VARCHAR(50)!");
                
                // Verify
                var newColumnInfo = jdbcTemplate.queryForMap(checkSql);
                String newType = (String) newColumnInfo.get("Type");
                log.info("✅ Verified new column type: {}", newType);
            } else {
                log.info("✅ alert_type column size is already correct: {}", currentType);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to migrate alert_type column: {}", e.getMessage(), e);
            // Don't throw - allow app to continue even if migration fails
        }
    }
}

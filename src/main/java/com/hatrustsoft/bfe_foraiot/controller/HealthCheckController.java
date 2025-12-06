package com.hatrustsoft.bfe_foraiot.controller;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.config.GracefulShutdownConfig;

import lombok.RequiredArgsConstructor;

/**
 * 🏥 HEALTH CHECK CONTROLLER
 * 
 * Cung cấp endpoint để Heroku và monitoring tools kiểm tra app health:
 * - /health - Basic health check (for Heroku)
 * - /health/detailed - Chi tiết về DB, Redis, MQTT
 * 
 * Giúp Heroku biết app còn sống và tránh kill sớm
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final DataSource dataSource;
    private final GracefulShutdownConfig shutdownConfig;

    @Value("${spring.application.name:BFE_forAIOT}")
    private String appName;

    /**
     * Basic health check - Heroku sử dụng endpoint này
     * Trả về nhanh để tránh timeout
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", shutdownConfig.isShuttingDown() ? "SHUTTING_DOWN" : "UP");
        health.put("app", appName);
        health.put("timestamp", System.currentTimeMillis());
        
        if (shutdownConfig.isShuttingDown()) {
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check - Kiểm tra tất cả dependencies
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("app", appName);
        health.put("timestamp", System.currentTimeMillis());
        
        // Check Database
        Map<String, Object> dbHealth = checkDatabase();
        health.put("database", dbHealth);
        
        // Overall status
        boolean isHealthy = "UP".equals(dbHealth.get("status")) && !shutdownConfig.isShuttingDown();
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Kiểm tra kết nối Database
     */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> dbHealth = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5); // 5 second timeout
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("responseTime", System.currentTimeMillis() - startTime + "ms");
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            dbHealth.put("responseTime", System.currentTimeMillis() - startTime + "ms");
        }
        
        return dbHealth;
    }
}

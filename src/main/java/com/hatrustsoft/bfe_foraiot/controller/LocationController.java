package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.AlertType;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.service.MemoryCacheService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
@Slf4j
public class LocationController {

    // ⏰ Đồng bộ với positioning-2d.html: 30 giây không nhận data → offline
    private static final long OFFLINE_THRESHOLD_SECONDS = 60; // 60 seconds - prevent flicker

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HelmetDataRepository helmetDataRepository;
    
    @Autowired
    private com.hatrustsoft.bfe_foraiot.service.RedisCacheService redisCacheService;
    
    @Autowired
    private MemoryCacheService memoryCacheService; // 🚀 Tối ưu: dùng cache thay vì query DB
    
    @Autowired
    private AlertRepository alertRepository; // 🚨 Để check pending alerts

    /**
     * ⭐ NEW: API lấy dữ liệu REALTIME từ Redis cache
     * Dùng cho location.html - hiển thị worker đang hoạt động
     * 
     * Logic:
     * - Nếu nhận data trong 30s: ACTIVE (màu xanh)
     * - Nếu 30s - 24h: INACTIVE (màu xám)
     * - Sau 24h: Tự động xóa khỏi Redis (không hiển thị)
     * 
     * 🚀 TỐI ƯU: Dùng MemoryCacheService.getEmployeeMap() thay vì N queries
     * 🚨 CHECK PENDING ALERTS: Hiển thị trạng thái FALL, HELP_REQUEST
     */
    @GetMapping("/map-data-realtime")
    public ResponseEntity<List<WorkerMapData>> getMapDataRealtime() {
        List<WorkerMapData> result = new ArrayList<>();

        // ✅ Lấy tất cả helmet data từ Redis (TTL 24h)
        List<HelmetData> cachedHelmets = redisCacheService.getAllActiveHelmets();
        
        // 🚀 TỐI ƯU: Lấy toàn bộ employee map từ cache (0 queries!)
        Map<String, Employee> employeeMap = memoryCacheService.getEmployeeMap();
        
        // 🚨 Lấy tất cả PENDING alerts để check trạng thái nguy hiểm
        List<Alert> pendingAlerts = alertRepository.findByStatusOrderByTriggeredAtDesc(AlertStatus.PENDING);
        Map<String, Alert> alertByMac = new HashMap<>();
        for (Alert alert : pendingAlerts) {
            if (alert.getHelmet() != null && alert.getHelmet().getMacAddress() != null) {
                String mac = alert.getHelmet().getMacAddress();
                // Ưu tiên FALL > HELP_REQUEST
                if (!alertByMac.containsKey(mac) || alert.getAlertType() == AlertType.FALL) {
                    alertByMac.put(mac, alert);
                }
            }
        }
        
        log.info("📡 Redis: {} helmets, Employees: {}, Pending alerts: {}", 
            cachedHelmets.size(), employeeMap.size(), pendingAlerts.size());

        // ⏰ Dùng VietnamTimeUtils.now() để đồng bộ timezone với receivedAt
        LocalDateTime now = com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils.now();

        // Map với employee data - KHÔNG CÓ DB QUERY trong loop!
        for (HelmetData data : cachedHelmets) {
            Employee emp = employeeMap.get(data.getMac()); // 🚀 Từ cache, không query DB
            
            WorkerMapData workerData = new WorkerMapData();
            if (emp != null) {
                workerData.setId(emp.getEmployeeId());
                workerData.setName(emp.getName());
                workerData.setPosition(emp.getPosition());
                workerData.setDepartment(emp.getDepartment());
            } else {
                // Nếu không tìm thấy employee, dùng data từ helmet
                workerData.setId(data.getEmployeeId() != null ? data.getEmployeeId() : data.getMac());
                workerData.setName(data.getEmployeeName() != null ? data.getEmployeeName() : "Worker " + data.getMac().substring(Math.max(0, data.getMac().length() - 4)));
                workerData.setPosition("Unknown");
                workerData.setDepartment("Unknown");
            }

            // ⭐ Xác định status dựa trên thời gian cập nhật
            LocalDateTime lastUpdate = data.getReceivedAt();
            String status = "ACTIVE";
            String alertType = null;
            
            if (lastUpdate != null) {
                long secondsAgo = java.time.temporal.ChronoUnit.SECONDS.between(lastUpdate, now);
                
                if (secondsAgo > OFFLINE_THRESHOLD_SECONDS) {
                    // Sau 30s không nhận data → INACTIVE (màu xám)
                    status = "INACTIVE";
                }
            }
            
            // 🚨 Check pending alert cho helmet này
            Alert pendingAlert = alertByMac.get(data.getMac());
            if (pendingAlert != null) {
                alertType = pendingAlert.getAlertType().name(); // FALL hoặc HELP_REQUEST
                status = "DANGER"; // Override status thành DANGER
                log.info("🚨 Worker {} has pending {}", data.getMac(), alertType);
            }

            // Tạo helmet info
            HelmetInfo helmet = new HelmetInfo();
            helmet.setHelmetId(data.getMac());
            helmet.setStatus(status); // ✅ ACTIVE, INACTIVE, hoặc DANGER
            helmet.setAlertType(alertType); // ✅ null, FALL, hoặc HELP_REQUEST
            helmet.setBatteryLevel(data.getBattery() != null ? data.getBattery().intValue() : 100);

            // Location
            LocationInfo location = new LocationInfo();
            location.setLatitude(data.getLat() != null ? data.getLat() : 0.0);
            location.setLongitude(data.getLon() != null ? data.getLon() : 0.0);
            helmet.setLastLocation(location);

            workerData.setHelmet(helmet);
            result.add(workerData);
        }

        log.info("📍 Realtime map data: {} workers from Redis", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * API trả về dữ liệu bản đồ cho location.html (từ DATABASE - legacy)
     * Format tương thích với code hiện tại
     */
    @GetMapping("/map-data")
    public ResponseEntity<List<WorkerMapData>> getMapData() {
        List<WorkerMapData> result = new ArrayList<>();

        // Lấy tất cả employees
        List<Employee> employees = employeeRepository.findAll();

        for (Employee emp : employees) {
            // Bỏ qua employee không có MAC
            if (emp.getMacAddress() == null || emp.getMacAddress().isEmpty()) {
                continue;
            }

            // Lấy dữ liệu helmet mới nhất từ MAC address
            HelmetData latestData = helmetDataRepository.findFirstByMacOrderByTimestampDesc(emp.getMacAddress())
                .orElse(null);
            
            if (latestData == null) {
                // Không có dữ liệu helmet cho employee này
                continue;
            }

            // Tạo object WorkerMapData
            WorkerMapData workerData = new WorkerMapData();
            workerData.setId(emp.getEmployeeId());
            workerData.setName(emp.getName());
            workerData.setPosition(emp.getPosition());
            workerData.setDepartment(emp.getDepartment());

            // Tạo helmet info
            HelmetInfo helmet = new HelmetInfo();
            helmet.setHelmetId(emp.getMacAddress());
            
            // Xác định status dựa trên battery và thời gian cập nhật
            String status = determineHelmetStatus(latestData);
            helmet.setStatus(status);
            
            helmet.setBatteryLevel(latestData.getBattery() != null ? 
                latestData.getBattery().intValue() : 0);

            // Location
            LocationInfo location = new LocationInfo();
            location.setLatitude(latestData.getLat() != null ? latestData.getLat() : 0.0);
            location.setLongitude(latestData.getLon() != null ? latestData.getLon() : 0.0);
            helmet.setLastLocation(location);

            workerData.setHelmet(helmet);
            result.add(workerData);
        }

        log.info("📍 Map data requested: {} workers with location", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * Xác định status của helmet dựa trên dữ liệu
     */
    private String determineHelmetStatus(HelmetData data) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = data.getReceivedAt() != null ? data.getReceivedAt() : data.getTimestamp();
        
        // ⏱️ Kiểm tra thời gian cập nhật - nếu quá 20 GIÂY thì coi như INACTIVE (màu xám)
        if (lastUpdate != null && lastUpdate.isBefore(now.minusSeconds(20))) {
            long secondsAgo = java.time.temporal.ChronoUnit.SECONDS.between(lastUpdate, now);
            log.debug("🕐 Helmet {} offline for {} seconds (threshold: 20s) -> INACTIVE", 
                data.getMac(), secondsAgo);
            return "INACTIVE";
        }

        // Kiểm tra battery
        if (data.getBattery() != null && data.getBattery() < 20.0) {
            log.debug("🔋 Helmet {} battery low: {}% -> ALERT", data.getMac(), data.getBattery());
            return "ALERT";
        }

        // Kiểm tra voltage
        if (data.getVoltage() != null && data.getVoltage() < 10.0) {
            log.debug("⚡ Helmet {} voltage low: {}V -> ALERT", data.getMac(), data.getVoltage());
            return "ALERT";
        }

        // Kiểm tra current
        if (data.getCurrent() != null && Math.abs(data.getCurrent()) > 50.0) {
            log.debug("⚠️ Helmet {} current abnormal: {}A -> ALERT", data.getMac(), data.getCurrent());
            return "ALERT";
        }

        return "ACTIVE";
    }

    /**
     * 🗑️ API xóa cache Redis cho một helmet cụ thể
     */
    @DeleteMapping("/cache/{mac}")
    public ResponseEntity<?> clearHelmetCache(@PathVariable String mac) {
        log.info("🗑️ Clearing cache for helmet: {}", mac);
        redisCacheService.removeHelmetData(mac);
        return ResponseEntity.ok("✅ Cache cleared for " + mac);
    }

    /**
     * 🗑️ API xóa TOÀN BỘ cache Redis
     */
    @DeleteMapping("/cache")
    public ResponseEntity<?> clearAllCache() {
        log.info("🗑️ Clearing ALL helmet cache");
        redisCacheService.clearAllCache();
        return ResponseEntity.ok("✅ All cache cleared");
    }

    // DTO Classes
    @Data
    public static class WorkerMapData {
        private String id;
        private String name;
        private String position;
        private String department;
        private HelmetInfo helmet;
    }

    @Data
    public static class HelmetInfo {
        private String helmetId;
        private String status; // ACTIVE, ALERT, INACTIVE, FALL, HELP_REQUEST
        private String alertType; // null, FALL, HELP_REQUEST
        private Integer batteryLevel;
        private LocationInfo lastLocation;
    }

    @Data
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
    }
}

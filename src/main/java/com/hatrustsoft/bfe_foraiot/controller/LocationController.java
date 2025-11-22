package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "*")
@Slf4j
public class LocationController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HelmetDataRepository helmetDataRepository;
    
    @Autowired
    private com.hatrustsoft.bfe_foraiot.service.RedisCacheService redisCacheService;

    /**
     * ⭐ NEW: API lấy dữ liệu REALTIME từ Redis cache
     * Dùng cho location.html - hiển thị worker đang hoạt động
     * 
     * Logic:
     * - Nếu nhận data trong 30s: ACTIVE (màu xanh)
     * - Nếu 30s - 12h: INACTIVE (màu xám)
     * - Sau 12h: Tự động xóa khỏi Redis (không hiển thị)
     */
    @GetMapping("/map-data-realtime")
    public ResponseEntity<List<WorkerMapData>> getMapDataRealtime() {
        List<WorkerMapData> result = new ArrayList<>();

        // ✅ Lấy tất cả helmet data từ Redis (TTL 12h)
        List<HelmetData> cachedHelmets = redisCacheService.getAllActiveHelmets();
        
        log.info("📡 Redis cache has {} helmets", cachedHelmets.size());

        LocalDateTime now = LocalDateTime.now();

        // Map với employee data
        for (HelmetData data : cachedHelmets) {
            Employee emp = employeeRepository.findByMacAddress(data.getMac()).orElse(null);
            
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
            LocalDateTime lastUpdate = data.getReceivedAt() != null ? data.getReceivedAt() : data.getTimestamp();
            String status = "ACTIVE";
            
            if (lastUpdate != null && lastUpdate.isBefore(now.minusSeconds(30))) {
                // Sau 30s không nhận data → INACTIVE (màu xám)
                long secondsAgo = java.time.temporal.ChronoUnit.SECONDS.between(lastUpdate, now);
                log.debug("🕐 Helmet {} offline for {} seconds -> INACTIVE", data.getMac(), secondsAgo);
                status = "INACTIVE";
            }

            // Tạo helmet info
            HelmetInfo helmet = new HelmetInfo();
            helmet.setHelmetId(data.getMac());
            helmet.setStatus(status); // ✅ ACTIVE hoặc INACTIVE dựa trên thời gian
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
        private String status; // ACTIVE, ALERT, INACTIVE
        private Integer batteryLevel;
        private LocationInfo lastLocation;
    }

    @Data
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
    }
}

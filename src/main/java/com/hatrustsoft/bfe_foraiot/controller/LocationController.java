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
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@Slf4j
public class LocationController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private HelmetDataRepository helmetDataRepository;

    /**
     * API trả về dữ liệu bản đồ cho location.html
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
        // Kiểm tra thời gian cập nhật - nếu quá 5 phút thì coi như INACTIVE
        if (data.getTimestamp() != null && 
            data.getTimestamp().isBefore(LocalDateTime.now().minusMinutes(5))) {
            return "INACTIVE";
        }

        // Kiểm tra battery
        if (data.getBattery() != null && data.getBattery() < 20.0) {
            return "ALERT";
        }

        // Kiểm tra voltage
        if (data.getVoltage() != null && data.getVoltage() < 10.0) {
            return "ALERT";
        }

        // Kiểm tra current
        if (data.getCurrent() != null && Math.abs(data.getCurrent()) > 50.0) {
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

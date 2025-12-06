package com.hatrustsoft.bfe_foraiot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverviewStats());
    }
    
    /**
     * 🔴 API lấy cảnh báo gần đây (hôm nay)
     */
    @GetMapping("/alerts/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentAlerts(limit));
    }
    
    /**
     * 🔋 API lấy trạng thái pin từ dữ liệu thực
     */
    @GetMapping("/battery-status")
    public ResponseEntity<List<Map<String, Object>>> getBatteryStatus() {
        return ResponseEntity.ok(dashboardService.getBatteryStatus());
    }

    @GetMapping("/realtime")
    public ResponseEntity<?> getRealtimeData() {
        return ResponseEntity.ok(dashboardService.getRealtimeData());
    }

    @GetMapping("/workers")
    public ResponseEntity<?> getWorkers(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(dashboardService.getWorkersList(status));
    }

    @GetMapping("/map-data")
    public ResponseEntity<?> getMapData() {
        return ResponseEntity.ok(dashboardService.getMapPositions());
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReports(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(dashboardService.generateReport(startDate, endDate));
    }
}


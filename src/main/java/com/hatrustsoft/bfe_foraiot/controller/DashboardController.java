package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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


package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.Alert;
import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.service.DashboardService;
import com.hatrustsoft.bfe_foraiot.service.PositioningService;
import com.hatrustsoft.bfe_foraiot.service.RedisCacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * 🎤 VOICE ASSISTANT API ENDPOINTS
 * 
 * Backend APIs cho Voice Assistant function calling
 * Các endpoints này được gọi bởi Gemini AI thông qua function tools
 */
@RestController
@RequestMapping("/api/voice")
@Slf4j
public class VoiceAssistantApiController {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private HelmetRepository helmetRepository;
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private RedisCacheService redisCacheService;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private PositioningService positioningService;
    
    /**
     * 🧑‍💼 Get all workers and their online status
     * Tool: get_workers
     */
    @GetMapping("/workers")
    public ResponseEntity<Map<String, Object>> getWorkers() {
        log.info("🎤 Voice API: get_workers");
        
        try {
            List<Employee> allEmployees = employeeRepository.findAll();
            List<HelmetData> activeHelmets = redisCacheService.getActiveHelmets();
            
            // Map helmet MAC to employee
            Map<String, Employee> helmetToEmployee = new HashMap<>();
            for (Employee emp : allEmployees) {
                if (emp.getHelmetMac() != null) {
                    helmetToEmployee.put(emp.getHelmetMac(), emp);
                }
            }
            
            // Build worker list with online status
            List<Map<String, Object>> workers = allEmployees.stream()
                .map(emp -> {
                    Map<String, Object> worker = new HashMap<>();
                    worker.put("id", emp.getId());
                    worker.put("name", emp.getName());
                    worker.put("department", emp.getDepartment());
                    worker.put("position", emp.getPosition());
                    worker.put("helmetMac", emp.getHelmetMac());
                    
                    // Check if online
                    boolean isOnline = emp.getHelmetMac() != null && 
                                      activeHelmets.stream()
                                          .anyMatch(h -> h.getMacAddress().equals(emp.getHelmetMac()));
                    worker.put("isOnline", isOnline);
                    
                    return worker;
                })
                .collect(Collectors.toList());
            
            long onlineCount = workers.stream().filter(w -> (Boolean) w.get("isOnline")).count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", workers.size());
            response.put("online", onlineCount);
            response.put("offline", workers.size() - onlineCount);
            response.put("workers", workers);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting workers: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 🚨 Get recent danger alerts (FALL, HELP_REQUEST)
     * Tool: get_recent_alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getRecentAlerts(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("🎤 Voice API: get_recent_alerts (limit={})", limit);
        
        try {
            List<Alert> allAlerts = alertRepository.findTop50ByOrderByTimestampDesc();
            
            // Filter danger alerts only
            List<Map<String, Object>> dangerAlerts = allAlerts.stream()
                .filter(alert -> "FALL".equals(alert.getAlertType()) || 
                               "HELP_REQUEST".equals(alert.getAlertType()))
                .limit(limit)
                .map(alert -> {
                    Map<String, Object> alertMap = new HashMap<>();
                    alertMap.put("id", alert.getId());
                    alertMap.put("type", alert.getAlertType());
                    alertMap.put("helmetMac", alert.getHelmetMac());
                    alertMap.put("timestamp", alert.getTimestamp().toString());
                    alertMap.put("resolved", alert.isResolved());
                    
                    // Get employee info
                    employeeRepository.findByHelmetMac(alert.getHelmetMac())
                        .ifPresent(emp -> {
                            alertMap.put("workerName", emp.getName());
                            alertMap.put("department", emp.getDepartment());
                        });
                    
                    return alertMap;
                })
                .collect(Collectors.toList());
            
            long unresolvedCount = dangerAlerts.stream()
                .filter(a -> !(Boolean) a.get("resolved"))
                .count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", dangerAlerts.size());
            response.put("unresolved", unresolvedCount);
            response.put("alerts", dangerAlerts);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting alerts: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 🪖 Get helmet status by MAC address
     * Tool: get_helmet_status
     */
    @GetMapping("/helmet")
    public ResponseEntity<Map<String, Object>> getHelmetStatus(
            @RequestParam String macAddress
    ) {
        log.info("🎤 Voice API: get_helmet_status (mac={})", macAddress);
        
        try {
            List<HelmetData> activeHelmets = redisCacheService.getActiveHelmets();
            
            HelmetData helmet = activeHelmets.stream()
                .filter(h -> h.getMacAddress().equalsIgnoreCase(macAddress))
                .findFirst()
                .orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("macAddress", macAddress);
            
            if (helmet != null) {
                response.put("isOnline", true);
                response.put("battery", helmet.getBatteryLevel());
                response.put("latitude", helmet.getLatitude());
                response.put("longitude", helmet.getLongitude());
                response.put("lastUpdate", helmet.getTimestamp().toString());
                
                // Get employee info
                employeeRepository.findByHelmetMac(macAddress)
                    .ifPresent(emp -> {
                        response.put("workerName", emp.getName());
                        response.put("department", emp.getDepartment());
                        response.put("position", emp.getPosition());
                    });
            } else {
                response.put("isOnline", false);
                response.put("message", "Helmet offline or not found");
                
                // Still get employee info if exists
                employeeRepository.findByHelmetMac(macAddress)
                    .ifPresent(emp -> {
                        response.put("workerName", emp.getName());
                        response.put("department", emp.getDepartment());
                    });
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting helmet status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 🗺️ Get all worker locations on map
     * Tool: get_map_data
     */
    @GetMapping("/map")
    public ResponseEntity<Map<String, Object>> getMapData() {
        log.info("🎤 Voice API: get_map_data");
        
        try {
            List<HelmetData> activeHelmets = redisCacheService.getActiveHelmets();
            
            List<Map<String, Object>> locations = activeHelmets.stream()
                .map(helmet -> {
                    Map<String, Object> loc = new HashMap<>();
                    loc.put("macAddress", helmet.getMacAddress());
                    loc.put("latitude", helmet.getLatitude());
                    loc.put("longitude", helmet.getLongitude());
                    loc.put("battery", helmet.getBatteryLevel());
                    
                    // Get employee info
                    employeeRepository.findByHelmetMac(helmet.getMacAddress())
                        .ifPresent(emp -> {
                            loc.put("workerName", emp.getName());
                            loc.put("department", emp.getDepartment());
                        });
                    
                    return loc;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", locations.size());
            response.put("locations", locations);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting map data: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 📊 Get dashboard overview
     * Tool: get_dashboard_overview
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        log.info("🎤 Voice API: get_dashboard_overview");
        
        try {
            // Get basic stats
            long totalWorkers = employeeRepository.count();
            List<HelmetData> activeHelmets = redisCacheService.getActiveHelmets();
            long activeCount = activeHelmets.size();
            
            // Get recent alerts
            List<Alert> recentAlerts = alertRepository.findTop10ByOrderByTimestampDesc();
            long unresolvedAlerts = recentAlerts.stream()
                .filter(a -> !a.isResolved())
                .count();
            
            // Battery status
            Map<String, Long> batteryStatus = activeHelmets.stream()
                .collect(Collectors.groupingBy(
                    h -> {
                        if (h.getBatteryLevel() > 50) return "good";
                        else if (h.getBatteryLevel() > 20) return "medium";
                        else return "low";
                    },
                    Collectors.counting()
                ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalWorkers", totalWorkers);
            response.put("activeWorkers", activeCount);
            response.put("offlineWorkers", totalWorkers - activeCount);
            response.put("unresolvedAlerts", unresolvedAlerts);
            response.put("batteryStatus", batteryStatus);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting dashboard overview: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}

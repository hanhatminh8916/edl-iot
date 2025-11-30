package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.model.Worker;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.repository.WorkerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final WorkerRepository workerRepository;
    private final EmployeeRepository employeeRepository;
    private final RedisCacheService redisCacheService;
    
    // Timeout để coi là offline (đồng bộ với các trang khác)
    private static final long OFFLINE_THRESHOLD_SECONDS = 30;

    /**
     * 📊 Lấy thống kê tổng quan từ dữ liệu THỰC
     */
    public Map<String, Object> getOverviewStats() {
        // Lấy tổng số công nhân từ bảng Employee
        long totalEmployees = employeeRepository.count();
        
        // Lấy số công nhân đang hoạt động từ Redis (online trong 30s)
        List<HelmetData> activeHelmets = redisCacheService.getAllActiveHelmets();
        LocalDateTime now = LocalDateTime.now();
        
        long activeWorkers = activeHelmets.stream()
            .filter(h -> h.getReceivedAt() != null)
            .filter(h -> ChronoUnit.SECONDS.between(h.getReceivedAt(), now) <= OFFLINE_THRESHOLD_SECONDS)
            .count();
        
        // Lấy cảnh báo hôm nay
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Alert> todayAlerts = alertRepository.findByTriggeredAtAfter(startOfDay);
        long pendingAlerts = todayAlerts.size();
        
        // Tính hiệu suất: % công nhân đang hoạt động / tổng số
        double efficiency = totalEmployees > 0 
            ? Math.round((double) activeWorkers / totalEmployees * 100) 
            : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkers", totalEmployees);
        stats.put("activeWorkers", activeWorkers);
        stats.put("pendingAlerts", pendingAlerts);
        stats.put("efficiency", efficiency);
        
        log.info("📊 Dashboard stats: total={}, active={}, alerts={}, efficiency={}%", 
            totalEmployees, activeWorkers, pendingAlerts, efficiency);

        return stats;
    }
    
    /**
     * 🔴 Lấy danh sách cảnh báo gần đây (hôm nay)
     */
    public List<Map<String, Object>> getRecentAlerts() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Alert> todayAlerts = alertRepository.findByTriggeredAtAfter(startOfDay);
        
        return todayAlerts.stream()
            .filter(a -> a.getTriggeredAt() != null)
            .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt())) // Mới nhất trước
            .limit(5)
            .map(alert -> {
                Map<String, Object> alertData = new HashMap<>();
                alertData.put("id", alert.getId());
                alertData.put("type", alert.getAlertType() != null ? alert.getAlertType().name() : "UNKNOWN");
                alertData.put("severity", alert.getSeverity() != null ? alert.getSeverity().name() : "LOW");
                alertData.put("message", alert.getMessage());
                alertData.put("timestamp", alert.getTriggeredAt().toString());
                alertData.put("time", alert.getTriggeredAt().toLocalTime().toString().substring(0, 5));
                
                // Lấy thông tin nhân viên
                if (alert.getHelmet() != null && alert.getHelmet().getWorker() != null) {
                    alertData.put("employeeName", alert.getHelmet().getWorker().getFullName());
                } else {
                    alertData.put("employeeName", "Không xác định");
                }
                
                return alertData;
            })
            .toList();
    }
    
    /**
     * 🔋 Lấy trạng thái pin từ Redis (dữ liệu thực)
     */
    public List<Map<String, Object>> getBatteryStatus() {
        List<HelmetData> helmets = redisCacheService.getAllActiveHelmets();
        List<Map<String, Object>> batteryList = new ArrayList<>();
        
        for (HelmetData helmet : helmets) {
            Map<String, Object> batteryData = new HashMap<>();
            
            // Tìm thông tin nhân viên
            Employee emp = employeeRepository.findByMacAddress(helmet.getMac()).orElse(null);
            
            if (emp != null) {
                batteryData.put("employeeName", emp.getName());
                batteryData.put("employeeId", emp.getEmployeeId());
                batteryData.put("initials", getInitials(emp.getName()));
            } else {
                batteryData.put("employeeName", "Helmet " + helmet.getMac().substring(Math.max(0, helmet.getMac().length() - 4)));
                batteryData.put("employeeId", helmet.getMac());
                batteryData.put("initials", "??");
            }
            
            batteryData.put("mac", helmet.getMac());
            batteryData.put("battery", helmet.getBattery() != null ? helmet.getBattery() : 0);
            batteryData.put("voltage", helmet.getVoltage() != null ? helmet.getVoltage() : 0);
            batteryData.put("current", helmet.getCurrent() != null ? Math.abs(helmet.getCurrent()) : 0);
            
            // Xác định trạng thái pin
            Double battery = helmet.getBattery();
            String batteryStatus;
            if (battery == null || battery <= 20) {
                batteryStatus = "low";
            } else if (battery <= 50) {
                batteryStatus = "medium";
            } else if (battery <= 80) {
                batteryStatus = "good";
            } else {
                batteryStatus = "excellent";
            }
            batteryData.put("batteryStatus", batteryStatus);
            
            batteryList.add(batteryData);
        }
        
        // Sắp xếp theo pin thấp nhất trước
        batteryList.sort((a, b) -> {
            Double ba = (Double) a.get("battery");
            Double bb = (Double) b.get("battery");
            return Double.compare(ba != null ? ba : 0, bb != null ? bb : 0);
        });
        
        return batteryList;
    }
    
    /**
     * Lấy chữ cái đầu của tên
     */
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    public Map<String, Object> getRealtimeData() {
        List<Helmet> activeHelmets = helmetRepository.findByStatus(HelmetStatus.ACTIVE);

        Map<String, Object> data = new HashMap<>();
        data.put("helmets", activeHelmets);
        data.put("timestamp", LocalDateTime.now());

        return data;
    }

    public List<Map<String, Object>> getWorkersList(String status) {
        // Build a worker view list that the frontend expects
        List<Worker> workers = workerRepository.findAll();
        List<Helmet> allHelmets = helmetRepository.findAll();

        return workers.stream().map(worker -> {
            Map<String, Object> w = new HashMap<>();
            w.put("id", worker.getId());
            w.put("name", worker.getFullName());
            w.put("employeeId", worker.getEmployeeId());
            w.put("position", worker.getPosition());
            w.put("department", worker.getDepartment());
            w.put("phone", worker.getPhoneNumber());

            // find helmet assigned to this worker (if any)
            Helmet helmet = allHelmets.stream()
                    .filter(h -> h.getWorker() != null && h.getWorker().getId().equals(worker.getId()))
                    .findFirst().orElse(null);

            if (helmet != null) {
                Map<String, Object> helmetMap = new HashMap<>();
                helmetMap.put("id", helmet.getId());
                helmetMap.put("helmetId", "HELMET-" + String.format("%03d", helmet.getHelmetId()));
                helmetMap.put("batteryLevel", helmet.getBatteryLevel() != null ? helmet.getBatteryLevel() : 0);
                helmetMap.put("status", helmet.getStatus() != null ? helmet.getStatus().name() : null);
                Map<String, Object> lastLocation = null;
                if (helmet.getLastLat() != null && helmet.getLastLon() != null) {
                    lastLocation = new HashMap<>();
                    lastLocation.put("latitude", helmet.getLastLat());
                    lastLocation.put("longitude", helmet.getLastLon());
                }
                helmetMap.put("lastLocation", lastLocation);
                w.put("helmet", helmetMap);
            } else {
                w.put("helmet", null);
            }

            return w;
        }).toList();
    }

    public List<Map<String, Object>> getMapPositions() {
        // Get all helmets with their workers
        List<Helmet> helmets = helmetRepository.findAll();
        
        return helmets.stream()
            .filter(helmet -> helmet.getWorker() != null) // Only helmets assigned to workers
            .map(helmet -> {
                Map<String, Object> workerData = new HashMap<>();
                Worker worker = helmet.getWorker();
                
                workerData.put("id", worker.getId());
                workerData.put("name", worker.getFullName());
                workerData.put("phone", worker.getPhoneNumber());
                workerData.put("employeeId", worker.getEmployeeId());
                workerData.put("position", worker.getPosition());
                
                // Helmet data
                Map<String, Object> helmetData = new HashMap<>();
                helmetData.put("helmetId", "HELMET-" + String.format("%03d", helmet.getHelmetId()));
                helmetData.put("status", helmet.getStatus());
                helmetData.put("batteryLevel", helmet.getBatteryLevel() != null ? helmet.getBatteryLevel() : 0);
                
                // Location data
                if (helmet.getLastLat() != null && helmet.getLastLon() != null) {
                    Map<String, Object> location = new HashMap<>();
                    location.put("latitude", helmet.getLastLat());
                    location.put("longitude", helmet.getLastLon());
                    helmetData.put("lastLocation", location);
                }
                
                workerData.put("helmet", helmetData);
                
                return workerData;
            })
            .toList();
    }

    public Map<String, Object> generateReport(String startDate, String endDate) {
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("generatedAt", LocalDateTime.now());

        // Get all workers with their helmets and statistics
        List<Worker> workers = workerRepository.findAll();
        List<Helmet> allHelmets = helmetRepository.findAll();
        
        List<Map<String, Object>> workerReports = workers.stream()
            .map(worker -> {
                Map<String, Object> workerData = new HashMap<>();
                workerData.put("id", worker.getId());
                workerData.put("name", worker.getFullName());
                workerData.put("employeeId", worker.getEmployeeId());
                workerData.put("position", worker.getPosition());
                workerData.put("department", worker.getDepartment());
                
                // Find helmet assigned to this worker
                Helmet helmet = allHelmets.stream()
                    .filter(h -> h.getWorker() != null && h.getWorker().getId().equals(worker.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (helmet != null) {
                    workerData.put("helmetId", "HELMET-" + String.format("%03d", helmet.getHelmetId()));
                    workerData.put("batteryLevel", helmet.getBatteryLevel());
                    workerData.put("status", helmet.getStatus());
                    
                    // Calculate working hours (mock calculation - in real system, track actual hours)
                    double workingHours = 8.0 + (Math.random() * 2.0 - 1.0); // 7-9 hours
                    workerData.put("workingHours", Math.round(workingHours * 10) / 10.0);
                    
                    // Calculate violations count from alerts
                    List<Alert> workerAlerts = alertRepository.findAll().stream()
                        .filter(a -> a.getHelmet() != null && a.getHelmet().getId().equals(helmet.getId()))
                        .toList();
                    workerData.put("violations", workerAlerts.size());
                    workerData.put("criticalAlerts", workerAlerts.stream()
                        .filter(a -> a.getSeverity().name().equals("CRITICAL"))
                        .count());
                    
                    // Calculate efficiency based on status and alerts
                    double efficiency = 95.0 - (workerAlerts.size() * 5.0);
                    if (efficiency < 50) efficiency = 50;
                    workerData.put("efficiency", Math.round(efficiency * 10) / 10.0);
                    
                    // Danger zone entries (mock data)
                    workerData.put("dangerZoneEntries", (int)(Math.random() * 5));
                    
                    // Fatigue level (mock data)
                    String[] fatigueLevels = {"Thấp", "Trung bình", "Cao"};
                    workerData.put("fatigueLevel", fatigueLevels[(int)(Math.random() * 3)]);
                } else {
                    workerData.put("helmetId", "Chưa gán");
                    workerData.put("workingHours", 0.0);
                    workerData.put("violations", 0);
                    workerData.put("efficiency", 0.0);
                    workerData.put("dangerZoneEntries", 0);
                    workerData.put("fatigueLevel", "N/A");
                }
                
                return workerData;
            })
            .toList();
        
        report.put("workers", workerReports);
        
        // Summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalWorkers", workers.size());
        
        // Count active workers by checking helmets
        long activeWorkers = allHelmets.stream()
            .filter(h -> h.getWorker() != null && h.getStatus() == HelmetStatus.ACTIVE)
            .count();
        summary.put("activeWorkers", activeWorkers);
        
        double totalHours = workerReports.stream()
            .mapToDouble(w -> (Double)w.getOrDefault("workingHours", 0.0))
            .sum();
        summary.put("totalWorkingHours", Math.round(totalHours * 10) / 10.0);
        
        double avgEfficiency = workerReports.stream()
            .mapToDouble(w -> (Double)w.getOrDefault("efficiency", 0.0))
            .average()
            .orElse(0.0);
        summary.put("averageEfficiency", Math.round(avgEfficiency * 10) / 10.0);
        
        long totalViolations = workerReports.stream()
            .mapToLong(w -> ((Integer)w.getOrDefault("violations", 0)).longValue())
            .sum();
        summary.put("totalViolations", totalViolations);
        
        long totalCriticalAlerts = workerReports.stream()
            .mapToLong(w -> ((Long)w.getOrDefault("criticalAlerts", 0L)))
            .sum();
        summary.put("totalCriticalAlerts", totalCriticalAlerts);
        
        report.put("summary", summary);
        
        return report;
    }
}

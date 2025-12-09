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
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final EmployeeRepository employeeRepository;
    private final RedisCacheService redisCacheService;
    private final MemoryCacheService memoryCacheService; // 🚀 TỐI ƯU: Dùng cache
    
    // Timeout để coi là offline (đồng bộ với các trang khác)
    private static final long OFFLINE_THRESHOLD_SECONDS = 30;

    /**
     * 📊 Lấy thống kê tổng quan từ dữ liệu THỰC
     * 🚀 TỐI ƯU: Dùng COUNT queries thay vì findAll
     */
    public Map<String, Object> getOverviewStats() {
        // 🚀 TỐI ƯU: Dùng count() thay vì findAll().size()
        long totalEmployees = employeeRepository.count();
        
        // Lấy số công nhân đang hoạt động từ Redis (online trong 30s)
        List<HelmetData> activeHelmets = redisCacheService.getAllActiveHelmets();
        LocalDateTime now = VietnamTimeUtils.now();
        
        long activeWorkers = activeHelmets.stream()
            .filter(h -> h.getReceivedAt() != null)
            .filter(h -> ChronoUnit.SECONDS.between(h.getReceivedAt(), now) <= OFFLINE_THRESHOLD_SECONDS)
            .count();
        
        // 🚀 TỐI ƯU: Dùng countByTriggeredAtAfter thay vì findByTriggeredAtAfter().size()
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long pendingAlerts = alertRepository.countByTriggeredAtAfter(startOfDay);
        
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
     * 🚀 TỐI ƯU: Query với LIMIT động và ORDER BY trực tiếp trong DB
     */
    public List<Map<String, Object>> getRecentAlerts(int limit) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        // Query alerts hôm nay, sắp xếp DESC
        List<Alert> todayAlerts = alertRepository.findByTriggeredAtAfter(startOfDay);
        
        return todayAlerts.stream()
            .filter(a -> a.getTriggeredAt() != null)
            .sorted((a1, a2) -> a2.getTriggeredAt().compareTo(a1.getTriggeredAt()))
            .limit(Math.min(limit, 50))
            .map(alert -> {
                Map<String, Object> alertData = new HashMap<>();
                alertData.put("id", alert.getId());
                alertData.put("type", alert.getAlertType() != null ? alert.getAlertType().name() : "UNKNOWN");
                alertData.put("severity", alert.getSeverity() != null ? alert.getSeverity().name() : "LOW");
                alertData.put("message", alert.getMessage());
                alertData.put("timestamp", alert.getTriggeredAt().toString());
                alertData.put("time", alert.getTriggeredAt().toLocalTime().toString().substring(0, 5));
                
                // Lấy thông tin nhân viên từ Helmet -> Employee
                if (alert.getHelmet() != null && alert.getHelmet().getEmployee() != null) {
                    alertData.put("employeeName", alert.getHelmet().getEmployee().getName());
                } else {
                    alertData.put("employeeName", "Không xác định");
                }
                
                return alertData;
            })
            .toList();
    }
    
    /**
     * 🔋 Lấy trạng thái pin từ Redis (dữ liệu thực)
     * 🚀 TỐI ƯU: Dùng MemoryCacheService thay vì query DB trong loop
     */
    public List<Map<String, Object>> getBatteryStatus() {
        List<HelmetData> helmets = redisCacheService.getAllActiveHelmets();
        List<Map<String, Object>> batteryList = new ArrayList<>();
        
        // 🚀 TỐI ƯU: Lấy toàn bộ employee map 1 lần
        Map<String, Employee> employeeMap = memoryCacheService.getEmployeeMap();
        
        for (HelmetData helmet : helmets) {
            Map<String, Object> batteryData = new HashMap<>();
            
            // 🚀 TỐI ƯU: Tìm từ cache thay vì query DB
            Employee emp = employeeMap.get(helmet.getMac());
            
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
        data.put("timestamp", VietnamTimeUtils.now());

        return data;
    }

    public List<Map<String, Object>> getWorkersList(String status) {
        // Build a worker view list that the frontend expects
        List<Employee> employees = employeeRepository.findAll();
        List<Helmet> allHelmets = helmetRepository.findAll();
        
        // 🚀 Lấy dữ liệu realtime từ Redis cache
        List<HelmetData> realtimeHelmets = redisCacheService.getAllActiveHelmets();
        Map<String, HelmetData> realtimeByMac = new HashMap<>();
        for (HelmetData data : realtimeHelmets) {
            if (data.getMac() != null) {
                realtimeByMac.put(data.getMac(), data);
            }
        }
        
        LocalDateTime now = VietnamTimeUtils.now();

        return employees.stream().map(employee -> {
            Map<String, Object> w = new HashMap<>();
            w.put("id", employee.getId());
            w.put("name", employee.getName());
            w.put("employeeId", employee.getEmployeeId());
            w.put("position", employee.getPosition());
            w.put("department", employee.getDepartment());
            w.put("phone", employee.getPhoneNumber());
            w.put("location", employee.getLocation());

            // find helmet assigned to this employee (if any)
            Helmet helmet = allHelmets.stream()
                    .filter(h -> h.getEmployee() != null && h.getEmployee().getId().equals(employee.getId()))
                    .findFirst().orElse(null);

            if (helmet != null) {
                Map<String, Object> helmetMap = new HashMap<>();
                helmetMap.put("id", helmet.getId());
                helmetMap.put("helmetId", "HELMET-" + String.format("%03d", helmet.getHelmetId()));
                
                // 🚀 Lấy battery và status từ Redis realtime
                HelmetData realtimeData = realtimeByMac.get(helmet.getMacAddress());
                if (realtimeData != null) {
                    // Có data realtime
                    helmetMap.put("batteryLevel", realtimeData.getBattery() != null ? realtimeData.getBattery().intValue() : 0);
                    
                    // Xác định status dựa trên thời gian update
                    String helmetStatus = "ACTIVE";
                    if (realtimeData.getReceivedAt() != null) {
                        long secondsAgo = java.time.temporal.ChronoUnit.SECONDS.between(realtimeData.getReceivedAt(), now);
                        if (secondsAgo > 30) {
                            helmetStatus = "INACTIVE";
                        }
                    }
                    helmetMap.put("status", helmetStatus);
                    
                    // Location từ realtime
                    if (realtimeData.getLat() != null && realtimeData.getLon() != null) {
                        Map<String, Object> lastLocation = new HashMap<>();
                        lastLocation.put("latitude", realtimeData.getLat());
                        lastLocation.put("longitude", realtimeData.getLon());
                        helmetMap.put("lastLocation", lastLocation);
                    }
                } else {
                    // Không có data realtime → lấy từ database
                    helmetMap.put("batteryLevel", helmet.getBatteryLevel() != null ? helmet.getBatteryLevel() : 0);
                    helmetMap.put("status", "OFFLINE"); // Không có data realtime = OFFLINE
                    
                    if (helmet.getLastLat() != null && helmet.getLastLon() != null) {
                        Map<String, Object> lastLocation = new HashMap<>();
                        lastLocation.put("latitude", helmet.getLastLat());
                        lastLocation.put("longitude", helmet.getLastLon());
                        helmetMap.put("lastLocation", lastLocation);
                    }
                }
                
                w.put("helmet", helmetMap);
            } else {
                w.put("helmet", null);
            }

            return w;
        }).toList();
    }

    public List<Map<String, Object>> getMapPositions() {
        // Get all helmets with their employees
        List<Helmet> helmets = helmetRepository.findAll();
        
        return helmets.stream()
            .filter(helmet -> helmet.getEmployee() != null) // Only helmets assigned to employees
            .map(helmet -> {
                Map<String, Object> employeeData = new HashMap<>();
                Employee employee = helmet.getEmployee();
                
                employeeData.put("id", employee.getId());
                employeeData.put("name", employee.getName());
                employeeData.put("phone", employee.getPhoneNumber());
                employeeData.put("employeeId", employee.getEmployeeId());
                employeeData.put("position", employee.getPosition());
                
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
                
                employeeData.put("helmet", helmetData);
                
                return employeeData;
            })
            .toList();
    }

    public Map<String, Object> generateReport(String startDate, String endDate) {
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("generatedAt", VietnamTimeUtils.now());

        // Get all employees with their helmets and statistics
        List<Employee> employees = employeeRepository.findAll();
        List<Helmet> allHelmets = helmetRepository.findAll();
        
        List<Map<String, Object>> employeeReports = employees.stream()
            .map(employee -> {
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("id", employee.getId());
                employeeData.put("name", employee.getName());
                employeeData.put("employeeId", employee.getEmployeeId());
                employeeData.put("position", employee.getPosition());
                employeeData.put("department", employee.getDepartment());
                
                // Find helmet assigned to this employee
                Helmet helmet = allHelmets.stream()
                    .filter(h -> h.getEmployee() != null && h.getEmployee().getId().equals(employee.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (helmet != null) {
                    employeeData.put("helmetId", "HELMET-" + String.format("%03d", helmet.getHelmetId()));
                    employeeData.put("batteryLevel", helmet.getBatteryLevel());
                    employeeData.put("status", helmet.getStatus());
                    
                    // Calculate working hours (mock calculation - in real system, track actual hours)
                    double workingHours = 8.0 + (Math.random() * 2.0 - 1.0); // 7-9 hours
                    employeeData.put("workingHours", Math.round(workingHours * 10) / 10.0);
                    
                    // Calculate violations count from alerts
                    List<Alert> employeeAlerts = alertRepository.findAll().stream()
                        .filter(a -> a.getHelmet() != null && a.getHelmet().getId().equals(helmet.getId()))
                        .toList();
                    employeeData.put("violations", employeeAlerts.size());
                    employeeData.put("criticalAlerts", employeeAlerts.stream()
                        .filter(a -> a.getSeverity() != null && a.getSeverity().name().equals("CRITICAL"))
                        .count());
                    
                    // Calculate efficiency based on status and alerts
                    double efficiency = 95.0 - (employeeAlerts.size() * 5.0);
                    if (efficiency < 50) efficiency = 50;
                    employeeData.put("efficiency", Math.round(efficiency * 10) / 10.0);
                    
                    // Danger zone entries (mock data)
                    employeeData.put("dangerZoneEntries", (int)(Math.random() * 5));
                    
                    // Fatigue level (mock data)
                    String[] fatigueLevels = {"Thấp", "Trung bình", "Cao"};
                    employeeData.put("fatigueLevel", fatigueLevels[(int)(Math.random() * 3)]);
                } else {
                    employeeData.put("helmetId", "Chưa gán");
                    employeeData.put("workingHours", 0.0);
                    employeeData.put("violations", 0);
                    employeeData.put("efficiency", 0.0);
                    employeeData.put("dangerZoneEntries", 0);
                    employeeData.put("fatigueLevel", "N/A");
                }
                
                return employeeData;
            })
            .toList();
        
        report.put("workers", employeeReports);
        
        // Summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalWorkers", employees.size());
        
        // Count active employees by checking helmets
        long activeEmployees = allHelmets.stream()
            .filter(h -> h.getEmployee() != null && h.getStatus() == HelmetStatus.ACTIVE)
            .count();
        summary.put("activeWorkers", activeEmployees);
        
        double totalHours = employeeReports.stream()
            .mapToDouble(w -> (Double)w.getOrDefault("workingHours", 0.0))
            .sum();
        summary.put("totalWorkingHours", Math.round(totalHours * 10) / 10.0);
        
        double avgEfficiency = employeeReports.stream()
            .mapToDouble(w -> (Double)w.getOrDefault("efficiency", 0.0))
            .average()
            .orElse(0.0);
        summary.put("averageEfficiency", Math.round(avgEfficiency * 10) / 10.0);
        
        long totalViolations = employeeReports.stream()
            .mapToLong(w -> ((Integer)w.getOrDefault("violations", 0)).longValue())
            .sum();
        summary.put("totalViolations", totalViolations);
        
        long totalCriticalAlerts = employeeReports.stream()
            .mapToLong(w -> ((Long)w.getOrDefault("criticalAlerts", 0L)))
            .sum();
        summary.put("totalCriticalAlerts", totalCriticalAlerts);
        
        report.put("summary", summary);
        
        return report;
    }
}


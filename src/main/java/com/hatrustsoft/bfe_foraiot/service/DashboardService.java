package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.model.Worker;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.repository.WorkerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final WorkerRepository workerRepository;

    public Map<String, Object> getOverviewStats() {
        List<Helmet> allHelmets = helmetRepository.findAll();
        List<Alert> pendingAlerts = alertRepository.findByStatus(AlertStatus.PENDING);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkers", allHelmets.size());
        stats.put("activeWorkers", allHelmets.stream().filter(h -> h.getStatus() == HelmetStatus.ACTIVE).count());
        stats.put("pendingAlerts", pendingAlerts.size());
        stats.put("criticalAlerts", pendingAlerts.stream().filter(a -> a.getSeverity().name().equals("CRITICAL")).count());

        return stats;
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

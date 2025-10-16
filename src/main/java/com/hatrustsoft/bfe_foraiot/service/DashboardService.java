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

    public List<Helmet> getWorkersList(String status) {
        if (status != null && !status.isEmpty()) {
            HelmetStatus helmetStatus = HelmetStatus.valueOf(status.toUpperCase());
            return helmetRepository.findByStatus(helmetStatus);
        }
        return helmetRepository.findAll();
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
        // Simplified report generation
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("generatedAt", LocalDateTime.now());

        return report;
    }
}

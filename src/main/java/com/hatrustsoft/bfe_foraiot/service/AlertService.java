package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertPublisher alertPublisher;

    public List<Alert> getPendingAlerts() {
        return alertRepository.findByStatus(AlertStatus.PENDING);
    }

    public List<Alert> getTodayAlerts() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return alertRepository.findByTriggeredAtAfter(startOfDay);
    }

    @Transactional
    public void acknowledgeAlert(Long id, String username) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(LocalDateTime.now());
            alert.setAcknowledgedBy(username);
            Alert saved = alertRepository.save(alert);
            
            // 📡 Push WebSocket update
            alertPublisher.publishAlertUpdate(saved);
        });
    }

    @Transactional
    public void resolveAlert(Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setStatus(AlertStatus.RESOLVED);
            Alert saved = alertRepository.save(alert);
            
            // 📡 Push WebSocket update
            alertPublisher.publishAlertUpdate(saved);
        });
    }

    public Map<String, Object> getStatistics(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Alert> recentAlerts = alertRepository.findByTriggeredAtAfter(since);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", recentAlerts.size());
        stats.put("pending", recentAlerts.stream().filter(a -> a.getStatus() == AlertStatus.PENDING).count());
        stats.put("acknowledged", recentAlerts.stream().filter(a -> a.getStatus() == AlertStatus.ACKNOWLEDGED).count());
        stats.put("resolved", recentAlerts.stream().filter(a -> a.getStatus() == AlertStatus.RESOLVED).count());

        return stats;
    }
}

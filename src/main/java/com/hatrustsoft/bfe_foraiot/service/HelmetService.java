package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hatrustsoft.bfe_foraiot.dto.HelmetDataDTO;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertSeverity;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.AlertType;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;

import lombok.RequiredArgsConstructor;

/**
 * DEPRECATED: This service is deprecated. MQTT data is now handled by MqttMessageHandler
 */
@Service
@RequiredArgsConstructor
public class HelmetService {

    private final HelmetRepository helmetRepository;
    private final HelmetDataRepository helmetDataRepository;
    private final AlertRepository alertRepository;
    private final WebSocketService webSocketService;
    private final AlertPublisher alertPublisher;

    @Transactional
    public void saveHelmetData(HelmetDataDTO dto) {
        // DEPRECATED - Now using MqttMessageHandler for real MQTT data
        // This method is kept for backward compatibility only
        throw new UnsupportedOperationException("This method is deprecated. Use MQTT integration instead.");
    }

    @Transactional
    public void createAlert(HelmetDataDTO dto) {
        Helmet helmet = helmetRepository.findByHelmetId(dto.getHelmetId())
                .orElseThrow();

        Alert alert = new Alert();
        alert.setHelmet(helmet);
        alert.setAlertType(mapEventToAlertType(dto.getEventType()));
        alert.setSeverity(calculateSeverity(dto));
        alert.setMessage(generateAlertMessage(dto));
        alert.setGpsLat(dto.getGpsLat());
        alert.setGpsLon(dto.getGpsLon());
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDING);

        Alert saved = alertRepository.save(alert);

        // 📡 Push WebSocket alert mới
        alertPublisher.publishNewAlert(saved);
        
        // Gửi alert realtime (legacy WebSocket)
        webSocketService.sendToAll("/topic/alerts", saved);
    }

    public List<Helmet> getAllActiveHelmets() {
        return helmetRepository.findByStatus(HelmetStatus.ACTIVE);
    }

    public List<Alert> getPendingAlerts() {
        return alertRepository.findByStatus(AlertStatus.PENDING);
    }

    private HelmetStatus mapEventToStatus(String eventType) {
        return switch (eventType) {
            case "FALL", "ABNORMAL" -> HelmetStatus.ALERT;
            case "NORMAL" -> HelmetStatus.ACTIVE;
            default -> HelmetStatus.INACTIVE;
        };
    }

    private AlertType mapEventToAlertType(String eventType) {
        return switch (eventType) {
            case "FALL" -> AlertType.FALL;
            case "PROXIMITY" -> AlertType.PROXIMITY;
            case "ABNORMAL" -> AlertType.ABNORMAL;
            default -> AlertType.OUT_OF_ZONE;
        };
    }

    private AlertSeverity calculateSeverity(HelmetDataDTO dto) {
        if ("FALL".equals(dto.getEventType())) {
            return AlertSeverity.CRITICAL;
        } else if ("PROXIMITY".equals(dto.getEventType()) && dto.getUwbDistance() < 1.0f) {
            return AlertSeverity.CRITICAL;
        } else if (dto.getBatteryLevel() < 10) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.INFO;
    }

    private String generateAlertMessage(HelmetDataDTO dto) {
        return switch (dto.getEventType()) {
            case "FALL" -> String.format("Công nhân #%d bị té ngã tại vị trí (%.6f, %.6f)",
                    dto.getHelmetId(), dto.getGpsLat(), dto.getGpsLon());
            case "PROXIMITY" -> String.format("Công nhân #%d quá gần phương tiện (%.1fm)",
                    dto.getHelmetId(), dto.getUwbDistance());
            default -> String.format("Cảnh báo từ công nhân #%d", dto.getHelmetId());
        };
    }

    public Helmet getHelmetById(Long id) {
        return helmetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Helmet not found with id: " + id));
    }

    public List<HelmetData> getHelmetHistory(Long id, int hours) {
        // DEPRECATED - New HelmetData entity doesn't have helmet relationship
        throw new UnsupportedOperationException("This method is deprecated. Query by MAC address instead.");
    }

    public void sendCommandToHelmet(Long id, com.hatrustsoft.bfe_foraiot.dto.CommandDTO command) {
        Helmet helmet = getHelmetById(id);
        // TODO: Implement MQTT command publishing
        // mqttTemplate.convertAndSend("helmet/" + helmet.getHelmetId() + "/command", command);
    }
}

package com.hatrustsoft.bfe_foraiot.service;

import com.hatrustsoft.bfe_foraiot.dto.HelmetDataDTO;
import com.hatrustsoft.bfe_foraiot.model.*;
import com.hatrustsoft.bfe_foraiot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HelmetService {

    private final HelmetRepository helmetRepository;
    private final HelmetDataRepository helmetDataRepository;
    private final AlertRepository alertRepository;
    private final WebSocketService webSocketService;

    @Transactional
    public void saveHelmetData(HelmetDataDTO dto) {
        // Tìm helmet
        Helmet helmet = helmetRepository.findByHelmetId(dto.getHelmetId())
                .orElseThrow(() -> new RuntimeException("Helmet not found"));

        // Cập nhật trạng thái helmet
        helmet.setLastLat(dto.getGpsLat());
        helmet.setLastLon(dto.getGpsLon());
        helmet.setBatteryLevel(dto.getBatteryLevel());
        helmet.setLastSeen(LocalDateTime.now());
        helmet.setStatus(mapEventToStatus(dto.getEventType()));
        helmetRepository.save(helmet);

        // Lưu dữ liệu chi tiết
        HelmetData data = new HelmetData();
        data.setHelmet(helmet);
        data.setTimestamp(LocalDateTime.now());
        data.setEventType(EventType.valueOf(dto.getEventType()));
        data.setGpsLat(dto.getGpsLat());
        data.setGpsLon(dto.getGpsLon());
        data.setBatteryLevel(dto.getBatteryLevel());
        data.setUwbDistance(dto.getUwbDistance());
        data.setRssi(dto.getRssi());
        data.setGatewayId(dto.getGatewayId());

        helmetDataRepository.save(data);
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

        alertRepository.save(alert);

        // Gửi alert realtime
        webSocketService.sendToAll("/topic/alerts", alert);
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
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return helmetDataRepository.findAll().stream()
                .filter(data -> data.getHelmet().getId().equals(id))
                .filter(data -> data.getTimestamp().isAfter(since))
                .toList();
    }

    public void sendCommandToHelmet(Long id, com.hatrustsoft.bfe_foraiot.dto.CommandDTO command) {
        Helmet helmet = getHelmetById(id);
        // TODO: Implement MQTT command publishing
        // mqttTemplate.convertAndSend("helmet/" + helmet.getHelmetId() + "/command", command);
    }
}

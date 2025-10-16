package com.hatrustsoft.bfe_foraiot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hatrustsoft.bfe_foraiot.dto.HelmetDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

// @Service - Tạm thời disable MQTT service
// Uncomment khi đã có MQTT broker
// @Service
@Slf4j
@RequiredArgsConstructor
public class MqttService {

    private final HelmetService helmetService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        try {
            String payload = message.getPayload().toString();
            log.info("Received MQTT: {}", payload);

            // Parse JSON
            HelmetDataDTO data = objectMapper.readValue(payload, HelmetDataDTO.class);

            // Lưu vào DB
            helmetService.saveHelmetData(data);

            // Phát hiện alert
            if (isAlertEvent(data)) {
                helmetService.createAlert(data);
            }

            // Gửi realtime qua WebSocket
            webSocketService.sendToAll("/topic/helmet-data", data);

        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }

    private boolean isAlertEvent(HelmetDataDTO data) {
        return "FALL".equals(data.getEventType()) ||
                "PROXIMITY".equals(data.getEventType()) ||
                data.getBatteryLevel() < 20;
    }
}

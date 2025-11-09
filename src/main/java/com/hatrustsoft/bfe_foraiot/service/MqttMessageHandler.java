package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MqttMessageHandler implements MessageHandler {

    @Autowired
    private HelmetDataRepository helmetDataRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MessengerService messengerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Ngưỡng cảnh báo nguy hiểm
    private static final double BATTERY_LOW_THRESHOLD = 20.0; // Pin < 20%
    private static final double VOLTAGE_LOW_THRESHOLD = 10.0; // Điện áp < 10V
    private static final double CURRENT_HIGH_THRESHOLD = 50.0; // Dòng điện > 50A

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            String payload = message.getPayload().toString();
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            
            log.info("📩 Received MQTT message from topic: {}", topic);
            log.info("📦 Payload: {}", payload);

            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(payload);

            // Tạo entity HelmetData
            HelmetData data = new HelmetData();
            data.setMac(jsonNode.get("mac").asText());
            data.setVoltage(jsonNode.has("voltage") ? jsonNode.get("voltage").asDouble() : null);
            data.setCurrent(jsonNode.has("current") ? jsonNode.get("current").asDouble() : null);
            data.setPower(jsonNode.has("power") ? jsonNode.get("power").asDouble() : null);
            data.setBattery(jsonNode.has("battery") ? jsonNode.get("battery").asDouble() : null);
            data.setLat(jsonNode.has("lat") ? jsonNode.get("lat").asDouble() : null);
            data.setLon(jsonNode.has("lon") ? jsonNode.get("lon").asDouble() : null);
            data.setCounter(jsonNode.has("counter") ? jsonNode.get("counter").asInt() : null);

            // Parse timestamp từ ESP32
            if (jsonNode.has("timestamp")) {
                String timestampStr = jsonNode.get("timestamp").asText();
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                data.setTimestamp(timestamp);
            } else {
                data.setTimestamp(LocalDateTime.now());
            }

            // Map MAC address -> Employee
            String macAddress = data.getMac();
            employeeRepository.findByMacAddress(macAddress).ifPresentOrElse(
                employee -> {
                    data.setEmployeeId(employee.getEmployeeId());
                    data.setEmployeeName(employee.getName());
                    log.info("👤 Mapped MAC {} to Employee: {} ({})", 
                             macAddress, employee.getName(), employee.getEmployeeId());
                },
                () -> {
                    data.setEmployeeId(null);
                    data.setEmployeeName(null);
                    log.warn("⚠️ No employee found for MAC: {}", macAddress);
                }
            );

            // Lưu vào database
            helmetDataRepository.save(data);
            log.info("✅ Saved helmet data: MAC={}, Battery={}%, Voltage={}V", 
                     data.getMac(), data.getBattery(), data.getVoltage());

            // Kiểm tra nguy hiểm và gửi cảnh báo
            checkDangerAndAlert(data);

        } catch (Exception e) {
            log.error("❌ Error processing MQTT message: {}", e.getMessage(), e);
        }
    }

    private void checkDangerAndAlert(HelmetData data) {
        StringBuilder alertMessage = new StringBuilder();
        boolean isDangerous = false;

        // Kiểm tra pin yếu
        if (data.getBattery() != null && data.getBattery() < BATTERY_LOW_THRESHOLD) {
            alertMessage.append(String.format("⚠️ Pin yếu: %.1f%%\n", data.getBattery()));
            isDangerous = true;
        }

        // Kiểm tra điện áp thấp
        if (data.getVoltage() != null && data.getVoltage() < VOLTAGE_LOW_THRESHOLD) {
            alertMessage.append(String.format("⚠️ Điện áp thấp: %.2fV\n", data.getVoltage()));
            isDangerous = true;
        }

        // Kiểm tra dòng điện cao
        if (data.getCurrent() != null && Math.abs(data.getCurrent()) > CURRENT_HIGH_THRESHOLD) {
            alertMessage.append(String.format("⚠️ Dòng điện bất thường: %.2fA\n", data.getCurrent()));
            isDangerous = true;
        }

        // Gửi cảnh báo qua Messenger nếu phát hiện nguy hiểm
        if (isDangerous) {
            String employeeInfo = data.getEmployeeName() != null 
                ? data.getEmployeeName() + " (" + data.getEmployeeId() + ")"
                : "MAC: " + data.getMac();

            String alertType = alertMessage.toString().trim();
            
            String location = String.format("%.6f, %.6f", 
                data.getLat() != null ? data.getLat() : 0.0,
                data.getLon() != null ? data.getLon() : 0.0);

            // Broadcast cảnh báo qua Messenger
            messengerService.broadcastDangerAlert(employeeInfo, alertType, location);
            log.warn("🚨 Danger alert broadcasted for MAC: {}", data.getMac());
        }
    }
}

package com.hatrustsoft.bfe_foraiot.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    // ===== SMART FILTERING CONFIG =====
    private static final long MIN_TIME_BETWEEN_SAVES_SECONDS = 10; // Tối thiểu 10 giây giữa các lần lưu
    private static final double MIN_DISTANCE_METERS = 5.0; // Di chuyển tối thiểu 5m mới lưu
    private static final double MIN_BATTERY_CHANGE = 1.0; // Pin thay đổi 1% mới lưu
    private static final double MIN_VOLTAGE_CHANGE = 0.5; // Voltage thay đổi 0.5V mới lưu

    // Cache để lưu dữ liệu cuối cùng của mỗi MAC
    private final Map<String, HelmetData> lastSavedData = new HashMap<>();
    private final Map<String, LocalDateTime> lastSavedTime = new HashMap<>();

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

            // ===== SMART FILTERING: Chỉ lưu khi cần thiết =====
            if (shouldSaveToDatabase(data)) {
                helmetDataRepository.save(data);
                
                // Cập nhật cache
                lastSavedData.put(macAddress, data);
                lastSavedTime.put(macAddress, LocalDateTime.now());
                
                log.info("✅ SAVED to DB: MAC={}, Battery={}%, Location=({}, {})", 
                         data.getMac(), data.getBattery(), data.getLat(), data.getLon());
            } else {
                log.debug("⏭️ SKIPPED save (no significant change): MAC={}, Battery={}%", 
                         data.getMac(), data.getBattery());
            }

            // Kiểm tra nguy hiểm và gửi cảnh báo (luôn kiểm tra, bất kể có lưu hay không)
            checkDangerAndAlert(data);

        } catch (Exception e) {
            log.error("❌ Error processing MQTT message: {}", e.getMessage(), e);
        }
    }

    /**
     * Quyết định có nên lưu data vào database hay không
     * Chỉ lưu khi:
     * 1. Chưa bao giờ lưu (lần đầu tiên)
     * 2. Đã qua >= 10 giây kể từ lần lưu cuối
     * 3. Di chuyển >= 5 mét
     * 4. Pin/voltage thay đổi đáng kể
     */
    private boolean shouldSaveToDatabase(HelmetData newData) {
        String mac = newData.getMac();
        
        // Lần đầu tiên nhận data từ MAC này → lưu
        if (!lastSavedData.containsKey(mac)) {
            log.info("🆕 First data from MAC: {} → SAVE", mac);
            return true;
        }

        HelmetData lastData = lastSavedData.get(mac);
        LocalDateTime lastTime = lastSavedTime.get(mac);
        LocalDateTime now = LocalDateTime.now();

        // 1️⃣ Kiểm tra thời gian: >= 10 giây
        long secondsSinceLastSave = Duration.between(lastTime, now).getSeconds();
        if (secondsSinceLastSave >= MIN_TIME_BETWEEN_SAVES_SECONDS) {
            log.info("⏰ Time passed: {}s >= {}s → SAVE", secondsSinceLastSave, MIN_TIME_BETWEEN_SAVES_SECONDS);
            return true;
        }

        // 2️⃣ Kiểm tra khoảng cách: >= 5 mét
        if (newData.getLat() != null && newData.getLon() != null 
            && lastData.getLat() != null && lastData.getLon() != null) {
            
            double distance = calculateDistance(
                lastData.getLat(), lastData.getLon(),
                newData.getLat(), newData.getLon()
            );
            
            if (distance >= MIN_DISTANCE_METERS) {
                log.info("📍 Distance: {}m >= {}m → SAVE", String.format("%.2f", distance), MIN_DISTANCE_METERS);
                return true;
            }
        }

        // 3️⃣ Kiểm tra thay đổi pin: >= 1%
        if (newData.getBattery() != null && lastData.getBattery() != null) {
            double batteryChange = Math.abs(newData.getBattery() - lastData.getBattery());
            if (batteryChange >= MIN_BATTERY_CHANGE) {
                log.info("🔋 Battery change: {}% >= {}% → SAVE", String.format("%.1f", batteryChange), MIN_BATTERY_CHANGE);
                return true;
            }
        }

        // 4️⃣ Kiểm tra thay đổi voltage: >= 0.5V
        if (newData.getVoltage() != null && lastData.getVoltage() != null) {
            double voltageChange = Math.abs(newData.getVoltage() - lastData.getVoltage());
            if (voltageChange >= MIN_VOLTAGE_CHANGE) {
                log.info("⚡ Voltage change: {}V >= {}V → SAVE", String.format("%.2f", voltageChange), MIN_VOLTAGE_CHANGE);
                return true;
            }
        }

        // Không có thay đổi đáng kể → không lưu
        log.debug("⏭️ No significant change → SKIP ({}s since last save)", secondsSinceLastSave);
        return false;
    }

    /**
     * Tính khoảng cách giữa 2 tọa độ GPS (Haversine formula)
     * @return Khoảng cách tính bằng mét
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // mét

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // Khoảng cách tính bằng mét
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
            
            double lat = Objects.requireNonNullElse(data.getLat(), 0.0);
            double lon = Objects.requireNonNullElse(data.getLon(), 0.0);
            String location = String.format("%.6f, %.6f", lat, lon);

            // Broadcast cảnh báo qua Messenger
            messengerService.broadcastDangerAlert(employeeInfo, alertType, location);
            log.warn("🚨 Danger alert broadcasted for MAC: {}", data.getMac());
        }
    }
}

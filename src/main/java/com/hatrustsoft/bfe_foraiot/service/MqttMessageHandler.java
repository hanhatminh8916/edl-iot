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

    @Autowired
    private RedisPublisherService redisPublisher; // ⭐ Thêm Redis Publisher

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Ngưỡng cảnh báo nguy hiểm
    private static final double BATTERY_LOW_THRESHOLD = 20.0; // Pin < 20%
    private static final double VOLTAGE_LOW_THRESHOLD = 10.0; // Điện áp < 10V
    private static final double CURRENT_HIGH_THRESHOLD = 50.0; // Dòng điện > 50A
    // ⭐ BỎ DANGER_ZONE_DISTANCE - Anchor = nguy hiểm rồi, không cần check distance

    // ===== SMART FILTERING CONFIG =====
    private static final long MIN_TIME_BETWEEN_SAVES_SECONDS = 10; // Tối thiểu 10 giây giữa các lần lưu
    private static final double MIN_DISTANCE_METERS = 5.0; // Di chuyển tối thiểu 5m mới lưu
    private static final double MIN_BATTERY_CHANGE = 1.0; // Pin thay đổi 1% mới lưu
    private static final double MIN_VOLTAGE_CHANGE = 0.5; // Voltage thay đổi 0.5V mới lưu

    // Cache để lưu dữ liệu cuối cùng của mỗi MAC
    private final Map<String, HelmetData> lastSavedData = new HashMap<>();
    private final Map<String, LocalDateTime> lastSavedTime = new HashMap<>();
    private final Map<String, LocalDateTime> lastDangerZoneAlert = new HashMap<>(); // ⭐ Cache cảnh báo anchor

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            String payload = message.getPayload().toString();
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            
            log.info("📩 Received MQTT from topic: {}", topic);
            log.debug("📦 Payload: {}", payload);

            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(payload);

            // ===== Parse basic helmet data =====
            HelmetData data = new HelmetData();
            data.setMac(jsonNode.get("mac").asText());
            data.setVoltage(jsonNode.has("voltage") ? jsonNode.get("voltage").asDouble() : null);
            data.setCurrent(jsonNode.has("current") ? jsonNode.get("current").asDouble() : null);
            data.setPower(jsonNode.has("power") ? jsonNode.get("power").asDouble() : null);
            data.setBattery(jsonNode.has("battery") ? jsonNode.get("battery").asDouble() : null);
            data.setLat(jsonNode.has("lat") ? jsonNode.get("lat").asDouble() : null);
            data.setLon(jsonNode.has("lon") ? jsonNode.get("lon").asDouble() : null);
            data.setCounter(jsonNode.has("counter") ? jsonNode.get("counter").asInt() : null);

            // ⭐ Parse metadata từ Gateway Python
            String mode = jsonNode.has("mode") ? jsonNode.get("mode").asText() : "direct";
            Boolean inDangerZone = jsonNode.has("inDangerZone") ? jsonNode.get("inDangerZone").asBoolean() : false;
            String dangerZoneId = jsonNode.has("dangerZone") ? jsonNode.get("dangerZone").asText() : null;
            Double distanceToAnchor = jsonNode.has("distance") ? jsonNode.get("distance").asDouble() : null;
            Double anchorLat = jsonNode.has("anchorLat") ? jsonNode.get("anchorLat").asDouble() : null;
            Double anchorLon = jsonNode.has("anchorLon") ? jsonNode.get("anchorLon").asDouble() : null;
            
            // ⭐ LoRa signal quality
            String gatewayMac = jsonNode.has("gateway") ? jsonNode.get("gateway").asText() : null;
            Integer rssi = jsonNode.has("rssi") ? jsonNode.get("rssi").asInt() : null;
            Double snr = jsonNode.has("snr") ? jsonNode.get("snr").asDouble() : null;

            // Log LoRa signal quality
            if (rssi != null && snr != null) {
                log.info("📶 LoRa Signal: RSSI={}dBm, SNR={}dB, Gateway={}", rssi, snr, gatewayMac);
                
                // Cảnh báo tín hiệu yếu
                if (rssi < -120) {
                    log.warn("⚠️ Weak LoRa signal: RSSI={}dBm (very weak)", rssi);
                }
            }

            // Parse timestamp từ ESP32/Gateway
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
                    log.info("👤 MAC {} → Employee: {} ({})", macAddress, employee.getName(), employee.getEmployeeId());
                },
                () -> {
                    data.setEmployeeId(null);
                    data.setEmployeeName(null);
                    log.warn("⚠️ No employee for MAC: {}", macAddress);
                }
            );

            // ⭐ LOGIC LƯU DỮ LIỆU dựa trên MODE
            boolean shouldSave;
            String saveReason;

            if (inDangerZone) {
                // 🚨 MODE ANCHOR: Lưu hết, không filter
                shouldSave = true;
                saveReason = "🚨 DANGER ZONE";
                log.warn("🚨 {} in danger zone: {}, distance: {}m", macAddress, dangerZoneId, distanceToAnchor);
            } else {
                // ✅ MODE DIRECT: Smart filtering
                shouldSave = shouldSaveToDatabase(data);
                saveReason = shouldSave ? "✅ SAVE" : "⏭️ SKIP";
            }

            if (shouldSave) {
                helmetDataRepository.save(data);
                lastSavedData.put(macAddress, data);
                lastSavedTime.put(macAddress, LocalDateTime.now());
                
                // ⭐ PUBLISH TO REDIS (sẽ tự động forward qua WebSocket)
                redisPublisher.publishHelmetData(data);
                
                log.info("{}: MAC={}, Mode={}, Battery={}%, Loc=({},{})", 
                         saveReason, macAddress, mode, data.getBattery(), data.getLat(), data.getLon());
            } else {
                log.debug("{}: MAC={}, Mode={}", saveReason, macAddress, mode);
            }

            // ⭐ Kiểm tra cảnh báo khu vực nguy hiểm (từ Anchor qua Gateway)
            if (inDangerZone && dangerZoneId != null && distanceToAnchor != null) {
                checkDangerZoneAlert(data, dangerZoneId, distanceToAnchor, anchorLat, anchorLon);
            }

            // Kiểm tra nguy hiểm thiết bị (pin, voltage, current)
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

    /**
     * ⭐ Cảnh báo khi vào khu vực nguy hiểm (từ Anchor qua Gateway)
     */
    private void checkDangerZoneAlert(HelmetData data, String dangerZoneId, 
                                      double distance, Double anchorLat, Double anchorLon) {
        String mac = data.getMac();
        LocalDateTime now = LocalDateTime.now();
        
        // Debounce: Chỉ cảnh báo mỗi 30s để tránh spam
        LocalDateTime lastAlert = lastDangerZoneAlert.get(mac);
        if (lastAlert != null && Duration.between(lastAlert, now).getSeconds() < 30) {
            log.debug("⏭️ Skip danger zone alert (debounce): MAC={}", mac);
            return;
        }

        // ⭐ BỎ CHECK DISTANCE - Phát hiện Anchor = đã nguy hiểm rồi!
        // Anchor chỉ đặt ở khu nguy hiểm, nên không cần check distance
        // distance chỉ để tham khảo mức độ nguy hiểm

        // Tạo message cảnh báo
        String employeeInfo = data.getEmployeeName() != null 
            ? data.getEmployeeName() + " (" + data.getEmployeeId() + ")"
            : "MAC: " + mac;

        StringBuilder alertMsg = new StringBuilder();
        alertMsg.append("🚨 CẢNH BÁO KHU VỰC NGUY HIỂM!\n");
        alertMsg.append("━━━━━━━━━━━━━━━━━━━━━━━━\n");
        alertMsg.append(String.format("👤 Nhân viên: %s\n", employeeInfo));
        alertMsg.append(String.format("⚓ Khu vực: %s\n", dangerZoneId));
        alertMsg.append(String.format("📏 Khoảng cách đến anchor: %.2fm\n", distance)); // ⭐ Chỉ hiển thị khoảng cách
        
        double battery = Objects.requireNonNullElse(data.getBattery(), 0.0);
        double voltage = Objects.requireNonNullElse(data.getVoltage(), 0.0);
        alertMsg.append(String.format("🔋 Pin: %.1f%%\n", battery));
        alertMsg.append(String.format("⚡ Điện áp: %.2fV\n", voltage));
        
        // Vị trí mũ
        double helmetLat = Objects.requireNonNullElse(data.getLat(), 0.0);
        double helmetLon = Objects.requireNonNullElse(data.getLon(), 0.0);
        alertMsg.append(String.format("📍 Vị trí mũ: %.6f, %.6f\n", helmetLat, helmetLon));
        
        // Vị trí anchor (nếu có)
        if (anchorLat != null && anchorLon != null) {
            alertMsg.append(String.format("⚓ Vị trí anchor: %.6f, %.6f\n", anchorLat, anchorLon));
        }

        String location = String.format("%.6f, %.6f", helmetLat, helmetLon);

        messengerService.broadcastDangerAlert(employeeInfo, alertMsg.toString(), location);
        lastDangerZoneAlert.put(mac, now);
        
        log.warn("🚨 DANGER ZONE ALERT: {} in {} at {}m", employeeInfo, dangerZoneId, distance);
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

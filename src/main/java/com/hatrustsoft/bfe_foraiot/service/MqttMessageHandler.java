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
import com.hatrustsoft.bfe_foraiot.dto.HelmetRealtimeDTO;
import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertSeverity;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.AlertType;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
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
    private AlertRepository alertRepository;
    
    @Autowired
    private AlertPublisher alertPublisher; // ⭐ Push alert qua WebSocket

    @Autowired
    private MessengerService messengerService;

    @Autowired
    private RedisPublisherService redisPublisher; // ⭐ Thêm Redis Publisher
    
    @Autowired
    private RedisCacheService redisCacheService; // ⭐ Redis Cache Service
    
    @Autowired
    private HelmetService helmetService; // ⭐ Thêm HelmetService để auto-create helmet
    
    @Autowired
    private PositioningService positioningService; // 🎯 Realtime UWB positioning

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Ngưỡng cảnh báo nguy hiểm
    private static final double BATTERY_LOW_THRESHOLD = 20.0; // Pin < 20%
    private static final double VOLTAGE_LOW_THRESHOLD = 10.0; // Điện áp < 10V
    private static final double CURRENT_HIGH_THRESHOLD = 50.0; // Dòng điện > 50A

    // ⭐ Alert debounce cache
    private final Map<String, LocalDateTime> lastDangerZoneAlert = new HashMap<>();
    private final Map<String, LocalDateTime> lastFallAlert = new HashMap<>();
    private final Map<String, LocalDateTime> lastHelpRequestAlert = new HashMap<>();
    
    // ⭐ IN-MEMORY CACHE để giảm DB queries
    private final Map<String, Employee> employeeCache = new HashMap<>(); // MAC → Employee
    private final Map<String, Helmet> helmetCache = new HashMap<>(); // MAC → Helmet
    private final Map<String, LocalDateTime> lastDbUpdate = new HashMap<>(); // MAC → last DB update time
    private static final long DB_UPDATE_INTERVAL_SECONDS = 30; // Chỉ update DB mỗi 30 giây
    
    // Debounce time cho alerts (30 giây)
    private static final long ALERT_DEBOUNCE_SECONDS = 30;

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
            String macAddress = jsonNode.get("mac").asText();
            data.setMac(macAddress);
            data.setVoltage(jsonNode.has("voltage") ? jsonNode.get("voltage").asDouble() : null);
            data.setCurrent(jsonNode.has("current") ? jsonNode.get("current").asDouble() : null);
            data.setPower(jsonNode.has("power") ? jsonNode.get("power").asDouble() : null);
            data.setBattery(jsonNode.has("battery") ? jsonNode.get("battery").asDouble() : null);
            
            // ✅ GPS HANDLING: Nếu lat/lon = 0 (mất GPS), giữ vị trí cũ từ Redis cache
            Double newLat = jsonNode.has("lat") ? jsonNode.get("lat").asDouble() : null;
            Double newLon = jsonNode.has("lon") ? jsonNode.get("lon").asDouble() : null;
            
            // Kiểm tra GPS có hợp lệ không (lat=0, lon=0 là mất GPS)
            boolean isGpsValid = newLat != null && newLon != null && 
                                 !(newLat == 0.0 && newLon == 0.0) &&
                                 Math.abs(newLat) <= 90 && Math.abs(newLon) <= 180;
            
            if (isGpsValid) {
                data.setLat(newLat);
                data.setLon(newLon);
            } else {
                // GPS mất tín hiệu - lấy vị trí cũ từ Redis cache
                HelmetData cachedData = redisCacheService.getHelmetData(macAddress);
                if (cachedData != null && cachedData.getLat() != null && cachedData.getLon() != null) {
                    data.setLat(cachedData.getLat());
                    data.setLon(cachedData.getLon());
                    log.warn("📍 GPS lost for MAC: {} - using cached position ({}, {})", 
                        macAddress, cachedData.getLat(), cachedData.getLon());
                } else {
                    // Không có cache, sử dụng null
                    data.setLat(null);
                    data.setLon(null);
                    log.warn("📍 GPS lost for MAC: {} - no cached position available", macAddress);
                }
            }
            
            data.setCounter(jsonNode.has("counter") ? jsonNode.get("counter").asInt() : null);

            // ⭐ Parse safety data (fallDetected, helpRequest)
            Integer fallDetected = jsonNode.has("fallDetected") ? jsonNode.get("fallDetected").asInt() : 0;
            Integer helpRequest = jsonNode.has("helpRequest") ? jsonNode.get("helpRequest").asInt() : 0;
            
            // ⭐ LOG CRITICAL: In ra giá trị fallDetected và helpRequest
            log.info("🔍 Safety Check - MAC: {}, fallDetected: {}, helpRequest: {}", 
                data.getMac(), fallDetected, helpRequest);
            
            Double temp = jsonNode.has("temp") ? jsonNode.get("temp").asDouble() : null;
            Double heartRate = jsonNode.has("hr") ? jsonNode.get("hr").asDouble() : null;
            Double spo2 = jsonNode.has("spo2") ? jsonNode.get("spo2").asDouble() : null;

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

            // ⭐ OPTIMIZED: Dùng cache để tránh query DB mỗi message
            Employee cachedEmployee = employeeCache.get(macAddress);
            if (cachedEmployee != null) {
                data.setEmployeeId(cachedEmployee.getEmployeeId());
                data.setEmployeeName(cachedEmployee.getName());
            } else {
                // Chỉ query DB khi chưa có cache
                employeeRepository.findByMacAddress(macAddress).ifPresentOrElse(
                    employee -> {
                        data.setEmployeeId(employee.getEmployeeId());
                        data.setEmployeeName(employee.getName());
                        employeeCache.put(macAddress, employee); // Cache lại
                        log.info("👤 MAC {} → Employee: {} (cached)", macAddress, employee.getName());
                    },
                    () -> {
                        data.setEmployeeId(null);
                        data.setEmployeeName(null);
                    }
                );
            }

            // ✅ CHỈ CACHE VÀO REDIS - KHÔNG LƯU VÀO DATABASE MỖI MESSAGE
            redisCacheService.cacheHelmetData(data);
            
            // ⭐ OPTIMIZED: Chỉ update DB mỗi 30 giây thay vì mỗi message
            LocalDateTime lastUpdate = lastDbUpdate.get(macAddress);
            LocalDateTime now = LocalDateTime.now();
            if (lastUpdate == null || Duration.between(lastUpdate, now).getSeconds() >= DB_UPDATE_INTERVAL_SECONDS) {
                // AUTO-CREATE HELMET if not exists (chỉ khi cần update DB)
                if (!helmetCache.containsKey(macAddress)) {
                    Helmet helmet = helmetService.findOrCreateHelmetByMac(macAddress);
                    helmetCache.put(macAddress, helmet);
                }
                
                // CẬP NHẬT VỊ TRÍ VÀO HELMETS TABLE
                helmetService.updateHelmetData(
                    macAddress, 
                    data.getBattery(), 
                    data.getLat(), 
                    data.getLon(), 
                    null
                );
                lastDbUpdate.put(macAddress, now);
                log.debug("💾 DB updated for MAC: {} (every 30s)", macAddress);
            }
            
            // ✅ LUÔN PUBLISH QUA REDIS → WEBSOCKET (cho realtime positioning)
            redisPublisher.publishHelmetData(data);;
            
            // 🎯 PUBLISH UWB DATA QUA WEBSOCKET CHO 2D POSITIONING (KHÔNG LƯU DB)
            JsonNode uwbNode = jsonNode.has("uwb") ? jsonNode.get("uwb") : null;
            if (uwbNode != null) {
                Map<String, Double> uwbData = positioningService.parseUwbData(uwbNode);
                boolean uwbReady = positioningService.isUwbReady(uwbNode);
                
                HelmetRealtimeDTO realtimeDTO = HelmetRealtimeDTO.builder()
                    .mac(macAddress)
                    .employeeId(data.getEmployeeId())
                    .employeeName(data.getEmployeeName())
                    .battery(data.getBattery())
                    .voltage(data.getVoltage())
                    .current(data.getCurrent())
                    .temp(temp)
                    .heartRate(heartRate)
                    .spo2(spo2)
                    .lat(data.getLat())
                    .lon(data.getLon())
                    .uwb(uwbData)
                    .uwbReady(uwbReady)
                    .fallDetected(fallDetected == 1)
                    .helpRequest(helpRequest == 1)
                    .status("online")
                    .timestamp(data.getTimestamp())
                    .receivedAt(LocalDateTime.now())
                    .build();
                
                // 📡 Stream realtime (không lưu vào DB/Redis)
                positioningService.publishRealtimePosition(realtimeDTO);
                
                log.info("📍 UWB Realtime: MAC={}, A0={}, A1={}, A2={}, Ready={}", 
                    macAddress, 
                    uwbData.get("A0"), uwbData.get("A1"), uwbData.get("A2"),
                    uwbReady);
            }
            
            log.info("📡 Realtime: MAC={}, Battery={}%, Loc=({},{}), Mode={}", 
                     macAddress, data.getBattery(), data.getLat(), data.getLon(), mode);

            // ⭐ CRITICAL: Kiểm tra ngã và SOS TRƯỚC TIÊN!
            log.info("⚡ Alert Check - fallDetected={}, helpRequest={}", fallDetected, helpRequest);
            
            if (fallDetected == 1) {
                log.warn("🚨 FALL DETECTED - Creating alert...");
                createFallDetectedAlert(data);
            }
            
            if (helpRequest == 1) {
                log.warn("🆘 HELP REQUEST - Creating alert...");
                createHelpRequestAlert(data);
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

    // ⭐ Debounce cache for danger alerts (battery, voltage, current)
    private final Map<String, LocalDateTime> lastDangerAlert = new HashMap<>();
    
    private void checkDangerAndAlert(HelmetData data) {
        String mac = data.getMac();
        LocalDateTime now = LocalDateTime.now();
        
        // ⭐ DEBOUNCE: Chỉ gửi cảnh báo mỗi 60 giây để giảm DB queries
        LocalDateTime lastAlert = lastDangerAlert.get(mac);
        if (lastAlert != null && Duration.between(lastAlert, now).getSeconds() < 60) {
            return; // Bỏ qua nếu đã gửi gần đây
        }
        
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
            lastDangerAlert.put(mac, now); // Ghi nhận thời điểm gửi;
            log.warn("🚨 Danger alert broadcasted for MAC: {}", data.getMac());
        }
    }
    
    /**
     * ⭐ Tạo cảnh báo khi phát hiện FALL (ngã)
     * Debounce: Chỉ tạo alert mới nếu > 30 giây kể từ alert trước
     */
    private void createFallDetectedAlert(HelmetData data) {
        try {
            String mac = data.getMac();
            LocalDateTime now = LocalDateTime.now();
            
            // ⭐ DEBOUNCE: Kiểm tra alert gần đây
            LocalDateTime lastAlert = lastFallAlert.get(mac);
            if (lastAlert != null && Duration.between(lastAlert, now).getSeconds() < ALERT_DEBOUNCE_SECONDS) {
                log.debug("⏭️ Skip duplicate fall alert (debounce: {}s since last)", 
                    Duration.between(lastAlert, now).getSeconds());
                return;
            }
            
            // Tìm helmet theo MAC
            Helmet helmet = helmetService.findOrCreateHelmetByMac(data.getMac());
            
            // Tạo Alert
            Alert alert = new Alert();
            alert.setHelmet(helmet);
            alert.setAlertType(AlertType.FALL);
            alert.setSeverity(AlertSeverity.CRITICAL);
            alert.setStatus(AlertStatus.PENDING);
            alert.setTriggeredAt(LocalDateTime.now());
            alert.setGpsLat(data.getLat());
            alert.setGpsLon(data.getLon());
            
            String employeeInfo = data.getEmployeeName() != null 
                ? data.getEmployeeName() + " (" + data.getEmployeeId() + ")"
                : "MAC: " + data.getMac();
            
            alert.setMessage(String.format("🚨 PHÁT HIỆN NGÃ: %s", employeeInfo));
            
            Alert saved = alertRepository.save(alert);
            
            // ⭐ Push alert qua WebSocket để frontend nhận realtime
            alertPublisher.publishNewAlert(saved);
            
            // Gửi thông báo qua Messenger
            double lat = Objects.requireNonNullElse(data.getLat(), 0.0);
            double lon = Objects.requireNonNullElse(data.getLon(), 0.0);
            String location = String.format("%.6f, %.6f", lat, lon);
            
            StringBuilder alertMsg = new StringBuilder();
            alertMsg.append("🚨 CẢNH BÁO KHẨN CẤP - PHÁT HIỆN NGÃ!\n");
            alertMsg.append("━━━━━━━━━━━━━━━━━━━━━━━━\n");
            alertMsg.append(String.format("👤 Nhân viên: %s\n", employeeInfo));
            alertMsg.append(String.format("📍 Vị trí: %.6f, %.6f\n", lat, lon));
            
            if (data.getBattery() != null) {
                alertMsg.append(String.format("🔋 Pin: %.1f%%\n", data.getBattery()));
            }
            
            alertMsg.append("⏰ Thời gian: ").append(LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )).append("\n");
            alertMsg.append("\n⚠️ VUI LÒNG KIỂM TRA NGAY LẬP TỨC!");
            
            messengerService.broadcastDangerAlert(employeeInfo, alertMsg.toString(), location);
            
            // ⭐ Cập nhật cache để debounce
            lastFallAlert.put(mac, now);
            
            log.error("🚨 FALL DETECTED: {} at ({}, {})", employeeInfo, lat, lon);
            
        } catch (Exception e) {
            log.error("❌ Error creating fall alert: {}", e.getMessage(), e);
        }
    }
    
    /**
     * ⭐ Tạo cảnh báo khi nhận được SOS (helpRequest)
     * Debounce: Chỉ tạo alert mới nếu > 30 giây kể từ alert trước
     */
    private void createHelpRequestAlert(HelmetData data) {
        try {
            String mac = data.getMac();
            LocalDateTime now = LocalDateTime.now();
            
            log.warn("🆘 createHelpRequestAlert() called for MAC: {}", mac);
            
            // ⭐ DEBOUNCE: Kiểm tra alert gần đây
            LocalDateTime lastAlert = lastHelpRequestAlert.get(mac);
            if (lastAlert != null && Duration.between(lastAlert, now).getSeconds() < ALERT_DEBOUNCE_SECONDS) {
                log.debug("⏭️ Skip duplicate help request alert (debounce: {}s since last)", 
                    Duration.between(lastAlert, now).getSeconds());
                return;
            }
            
            log.info("✅ Creating HELP_REQUEST alert...");
            
            // Tìm helmet theo MAC
            log.info("🔍 Finding helmet for MAC: {}", data.getMac());
            Helmet helmet = helmetService.findOrCreateHelmetByMac(data.getMac());
            log.info("✅ Helmet found/created - ID: {}, Helmet ID: {}", 
                helmet != null ? helmet.getId() : "NULL",
                helmet != null ? helmet.getHelmetId() : "NULL");
            
            if (helmet == null) {
                log.error("❌ CRITICAL: Helmet is NULL for MAC: {}", data.getMac());
                throw new RuntimeException("Failed to find/create helmet for MAC: " + data.getMac());
            }
            
            // Tạo Alert
            log.info("🏗️ Creating Alert object...");
            Alert alert = new Alert();
            alert.setHelmet(helmet);
            alert.setAlertType(AlertType.HELP_REQUEST); // ⭐ Sử dụng HELP_REQUEST cho SOS
            alert.setSeverity(AlertSeverity.CRITICAL);
            alert.setStatus(AlertStatus.PENDING);
            alert.setTriggeredAt(LocalDateTime.now());
            alert.setGpsLat(data.getLat());
            alert.setGpsLon(data.getLon());
            
            String employeeInfo = data.getEmployeeName() != null 
                ? data.getEmployeeName() + " (" + data.getEmployeeId() + ")"
                : "MAC: " + data.getMac();
            
            alert.setMessage(String.format("🆘 YÊU CẦU TRỢ GIÚP: %s", employeeInfo));
            log.info("✅ Alert object created with message: {}", alert.getMessage());
            
            // ⭐ LƯU VÀO DATABASE
            log.info("💾 Saving HELP_REQUEST alert to database...");
            Alert saved = null;
            try {
                saved = alertRepository.save(alert);
                log.info("✅ HELP_REQUEST alert saved successfully - ID: {}, Type: {}, Severity: {}", 
                    saved.getId(), saved.getAlertType(), saved.getSeverity());
            } catch (Exception saveEx) {
                log.error("❌ CRITICAL: Failed to save HELP_REQUEST alert to database", saveEx);
                throw saveEx; // Re-throw để thấy lỗi
            }
            
            // ⭐ Push alert qua WebSocket để frontend nhận realtime
            try {
                alertPublisher.publishNewAlert(saved);
                log.info("📡 HELP_REQUEST alert published via WebSocket");
            } catch (Exception wsEx) {
                log.error("⚠️ Failed to publish HELP_REQUEST alert via WebSocket: {}", wsEx.getMessage());
                // Không throw, vì đã lưu DB thành công
            }
            
            // Gửi thông báo qua Messenger
            double lat = Objects.requireNonNullElse(data.getLat(), 0.0);
            double lon = Objects.requireNonNullElse(data.getLon(), 0.0);
            String location = String.format("%.6f, %.6f", lat, lon);
            
            StringBuilder alertMsg = new StringBuilder();
            alertMsg.append("🆘 CẢNH BÁO KHẨN CẤP - YÊU CẦU TRỢ GIÚP!\n");
            alertMsg.append("━━━━━━━━━━━━━━━━━━━━━━━━\n");
            alertMsg.append(String.format("👤 Nhân viên: %s\n", employeeInfo));
            alertMsg.append(String.format("📍 Vị trí: %.6f, %.6f\n", lat, lon));
            
            if (data.getBattery() != null) {
                alertMsg.append(String.format("🔋 Pin: %.1f%%\n", data.getBattery()));
            }
            
            alertMsg.append("⏰ Thời gian: ").append(LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            )).append("\n");
            alertMsg.append("\n⚠️ NHÂN VIÊN CẦN TRỢ GIÚP NGAY!");
            
            messengerService.broadcastDangerAlert(employeeInfo, alertMsg.toString(), location);
            
            // ⭐ Cập nhật cache để debounce
            lastHelpRequestAlert.put(mac, now);
            
            log.error("🆘 HELP REQUEST ALERT CREATED: {} at ({}, {})", employeeInfo, lat, lon);
            
        } catch (Exception e) {
            log.error("❌❌❌ CRITICAL ERROR creating help request alert for MAC {}: {}", 
                data.getMac(), e.getMessage(), e);
            // In ra full stack trace để debug
            e.printStackTrace();
        }
    }
}

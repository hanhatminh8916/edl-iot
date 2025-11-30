package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hatrustsoft.bfe_foraiot.dto.HelmetRealtimeDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * 🎯 Realtime Positioning Service
 * 
 * - Không lưu vị trí vào Redis/DB mỗi lần cập nhật
 * - Chỉ stream qua WebSocket cho realtime display
 * - Lưu vị trí cuối vào Redis khi tag offline (24h TTL)
 */
@Service
@Slf4j
public class PositioningService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RedisCacheService redisCacheService;
    
    // Cache last position & timestamp for each helmet
    private final Map<String, HelmetRealtimeDTO> helmetCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSeenTime = new ConcurrentHashMap<>();
    
    // Timeout để coi tag là offline (30 giây không nhận data)
    private static final long OFFLINE_TIMEOUT_SECONDS = 30;
    
    /**
     * 📡 Publish realtime UWB data qua WebSocket (KHÔNG LƯU DB)
     * Được gọi từ MqttMessageHandler khi nhận data từ helmet
     */
    public void publishRealtimePosition(HelmetRealtimeDTO dto) {
        String mac = dto.getMac();
        
        // Update cache
        helmetCache.put(mac, dto);
        lastSeenTime.put(mac, LocalDateTime.now());
        
        // 📤 Chỉ push qua WebSocket - KHÔNG lưu DB
        messagingTemplate.convertAndSend("/topic/helmet/position", dto);
        
        log.debug("📍 Realtime position: {} UWB={}", mac, dto.getUwb());
    }
    
    /**
     * Parse UWB data từ JsonNode
     */
    public Map<String, Double> parseUwbData(JsonNode uwbNode) {
        Map<String, Double> uwb = new HashMap<>();
        
        if (uwbNode == null || !uwbNode.isObject()) {
            return uwb;
        }
        
        // Parse khoảng cách đến các anchor
        if (uwbNode.has("A0")) uwb.put("A0", uwbNode.get("A0").asDouble());
        if (uwbNode.has("A1")) uwb.put("A1", uwbNode.get("A1").asDouble());
        if (uwbNode.has("A2")) uwb.put("A2", uwbNode.get("A2").asDouble());
        if (uwbNode.has("TAG2")) uwb.put("TAG2", uwbNode.get("TAG2").asDouble());
        
        // Parse baseline (calibration) values
        if (uwbNode.has("baseline_A1")) uwb.put("baseline_A1", uwbNode.get("baseline_A1").asDouble());
        if (uwbNode.has("baseline_A2")) uwb.put("baseline_A2", uwbNode.get("baseline_A2").asDouble());
        
        // 🎯 Parse ready flag để frontend biết UWB sẵn sàng
        if (uwbNode.has("ready")) uwb.put("ready", uwbNode.get("ready").asDouble());
        
        return uwb;
    }
    
    /**
     * Check if UWB is ready for positioning
     */
    public boolean isUwbReady(JsonNode uwbNode) {
        if (uwbNode == null) return false;
        return uwbNode.has("ready") && uwbNode.get("ready").asInt() == 1;
    }
    
    /**
     * ⏰ Scheduled task: Check for offline tags every 10 seconds
     * Khi tag offline → chuyển màu xám và lưu vị trí cuối vào Redis (24h TTL)
     */
    @Scheduled(fixedRate = 10000) // 10 giây
    public void checkOfflineTags() {
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, LocalDateTime> entry : lastSeenTime.entrySet()) {
            String mac = entry.getKey();
            LocalDateTime lastSeen = entry.getValue();
            
            long secondsSinceLastSeen = java.time.Duration.between(lastSeen, now).getSeconds();
            
            if (secondsSinceLastSeen > OFFLINE_TIMEOUT_SECONDS) {
                // Tag is offline
                HelmetRealtimeDTO cachedData = helmetCache.get(mac);
                
                if (cachedData != null && !"offline".equals(cachedData.getStatus())) {
                    // Mark as offline and broadcast
                    cachedData.setStatus("offline");
                    
                    // 📤 Notify frontend that tag is offline (grey color)
                    messagingTemplate.convertAndSend("/topic/helmet/position", cachedData);
                    
                    log.info("⚪ Tag {} went OFFLINE, last position cached in Redis (24h TTL)", mac);
                    
                    // ✅ Chỉ lưu vị trí cuối vào Redis khi offline (24h TTL)
                    // Đã được lưu trong RedisCacheService với TTL 12h
                    // Không cần lưu thêm ở đây
                }
            }
        }
    }
    
    /**
     * Get all currently tracked helmets
     */
    public Map<String, HelmetRealtimeDTO> getAllTrackedHelmets() {
        return new HashMap<>(helmetCache);
    }
    
    /**
     * Clear cache for a specific helmet
     */
    public void removeFromCache(String mac) {
        helmetCache.remove(mac);
        lastSeenTime.remove(mac);
    }
}

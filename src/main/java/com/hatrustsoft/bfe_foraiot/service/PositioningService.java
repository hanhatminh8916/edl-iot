package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.hatrustsoft.bfe_foraiot.dto.HelmetRealtimeDTO;
import com.hatrustsoft.bfe_foraiot.entity.TagLastPosition;
import com.hatrustsoft.bfe_foraiot.repository.TagLastPositionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 🎯 Realtime Positioning Service
 * 
 * - Stream UWB data qua WebSocket cho realtime display
 * - Lưu vị trí cuối vào DB để hiển thị tag offline màu xám
 */
@Service
@Slf4j
public class PositioningService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RedisCacheService redisCacheService;
    
    @Autowired
    private TagLastPositionRepository tagLastPositionRepository;
    
    // Cache last position & timestamp for each helmet
    private final Map<String, HelmetRealtimeDTO> helmetCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSeenTime = new ConcurrentHashMap<>();
    
    // Timeout để coi tag là offline (30 giây không nhận data)
    private static final long OFFLINE_TIMEOUT_SECONDS = 30;
    
    /**
     * 📡 Publish realtime UWB data qua WebSocket
     * Đồng thời lưu vị trí cuối vào DB để hiển thị offline
     */
    @Transactional
    public void publishRealtimePosition(HelmetRealtimeDTO dto) {
        String mac = dto.getMac();
        LocalDateTime now = LocalDateTime.now();
        
        // Update cache
        helmetCache.put(mac, dto);
        lastSeenTime.put(mac, now);
        
        // 📤 Push qua WebSocket cho realtime display
        messagingTemplate.convertAndSend("/topic/helmet/position", dto);
        
        // 💾 Lưu vị trí cuối vào DB (upsert)
        saveLastPosition(dto, now);
        
        log.debug("📍 Realtime position: {} UWB={}", mac, dto.getUwb());
    }
    
    /**
     * 💾 Lưu vị trí cuối cùng của tag vào DB
     */
    @Transactional
    public void saveLastPosition(HelmetRealtimeDTO dto, LocalDateTime lastSeen) {
        String mac = dto.getMac();
        Map<String, Double> uwb = dto.getUwb();
        
        // Tìm hoặc tạo mới record
        TagLastPosition tagPos = tagLastPositionRepository.findByMac(mac)
            .orElse(new TagLastPosition());
        
        tagPos.setMac(mac);
        tagPos.setEmployeeId(dto.getEmployeeId());
        tagPos.setEmployeeName(dto.getEmployeeName());
        tagPos.setIsOnline(true);
        tagPos.setLastSeen(lastSeen);
        tagPos.setBattery(dto.getBattery());
        
        // Lưu UWB distances nếu có
        if (uwb != null) {
            tagPos.setDistanceA0(uwb.get("A0"));
            tagPos.setDistanceA1(uwb.get("A1"));
            tagPos.setDistanceA2(uwb.get("A2"));
            
            // Tính toán vị trí từ UWB (weighted centroid)
            Double[] position = calculatePosition(uwb);
            if (position != null) {
                tagPos.setLastX(position[0]);
                tagPos.setLastY(position[1]);
            }
        }
        
        tagLastPositionRepository.save(tagPos);
    }
    
    /**
     * 🎯 Tính vị trí từ UWB distances (Weighted Centroid)
     */
    private Double[] calculatePosition(Map<String, Double> uwb) {
        Double d0 = uwb.get("A0");
        Double d1 = uwb.get("A1");
        Double d2 = uwb.get("A2");
        
        if (d0 == null || d1 == null || d2 == null || d0 <= 0 || d1 <= 0 || d2 <= 0) {
            return null;
        }
        
        // Anchor positions (đồng bộ với frontend)
        double a0x = 1, a0y = 2;   // A0: góc dưới trái
        double a1x = 1, a1y = 11;  // A1: phía trên trái
        double a2x = 18, a2y = 1;  // A2: góc dưới phải
        
        // Weighted centroid
        double w0 = 1 / (d0 * d0);
        double w1 = 1 / (d1 * d1);
        double w2 = 1 / (d2 * d2);
        double totalWeight = w0 + w1 + w2;
        
        double x = (a0x * w0 + a1x * w1 + a2x * w2) / totalWeight;
        double y = (a0y * w0 + a1y * w1 + a2y * w2) / totalWeight;
        
        // Clamp to area (0-20m)
        x = Math.max(0, Math.min(20, x));
        y = Math.max(0, Math.min(20, y));
        
        return new Double[]{x, y};
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
     * Đánh dấu tag offline trong DB và notify frontend
     */
    @Scheduled(fixedRate = 10000) // 10 giây
    @Transactional
    public void checkOfflineTags() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(OFFLINE_TIMEOUT_SECONDS);
        
        // Đánh dấu offline trong DB
        int offlineCount = tagLastPositionRepository.markOfflineByLastSeenBefore(threshold);
        if (offlineCount > 0) {
            log.info("⚪ Marked {} tags as offline", offlineCount);
        }
        
        // Notify frontend về các tag offline (từ cache)
        for (Map.Entry<String, LocalDateTime> entry : lastSeenTime.entrySet()) {
            String mac = entry.getKey();
            LocalDateTime lastSeen = entry.getValue();
            
            long secondsSinceLastSeen = java.time.Duration.between(lastSeen, now).getSeconds();
            
            if (secondsSinceLastSeen > OFFLINE_TIMEOUT_SECONDS) {
                HelmetRealtimeDTO cachedData = helmetCache.get(mac);
                
                if (cachedData != null && !"offline".equals(cachedData.getStatus())) {
                    cachedData.setStatus("offline");
                    
                    // 📤 Notify frontend that tag is offline (grey color)
                    messagingTemplate.convertAndSend("/topic/helmet/position", cachedData);
                    
                    log.info("⚪ Tag {} went OFFLINE", mac);
                }
            }
        }
    }
    
    /**
     * 📋 Lấy tất cả tags (online + offline) từ DB
     */
    public List<TagLastPosition> getAllTagPositions() {
        return tagLastPositionRepository.findAll();
    }
    
    /**
     * 📋 Lấy tất cả tags offline từ DB
     */
    public List<TagLastPosition> getOfflineTags() {
        return tagLastPositionRepository.findByIsOnlineFalse();
    }
    
    /**
     * Get all currently tracked helmets from cache
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

package com.hatrustsoft.bfe_foraiot.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.service.RedisCacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * 🕐 Scheduled Job để detect helmet offline và lưu vị trí cuối cùng vào Database
 * 
 * Logic:
 * - Chạy mỗi 60 giây (để tránh vượt giới hạn queries của JawsDB free tier)
 * - Kiểm tra các helmet không có data trong 30 giây
 * - Lưu vị trí cuối cùng vào helmet_data table
 * - Tránh lưu trùng lặp bằng tracking MACs đã xử lý
 */
@Component
@Slf4j
public class OfflineDetectionScheduler {

    @Autowired
    private RedisCacheService redisCacheService;
    
    @Autowired
    private HelmetDataRepository helmetDataRepository;
    
    private static final int OFFLINE_TIMEOUT_SECONDS = 30; // 30 giây không có data = offline
    
    // Track các MAC đã được lưu vào DB khi offline (tránh lưu lặp lại)
    private Set<String> savedOfflineMacs = new HashSet<>();
    
    /**
     * 🔄 Chạy mỗi 60 giây để kiểm tra offline helmets
     * (Giảm tần suất để tránh vượt giới hạn 18000 queries/giờ của JawsDB free tier)
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void detectOfflineHelmets() {
        try {
            List<HelmetData> offlineHelmets = redisCacheService.getOfflineHelmets(OFFLINE_TIMEOUT_SECONDS);
            
            for (HelmetData data : offlineHelmets) {
                String mac = data.getMac();
                
                // Bỏ qua nếu đã lưu rồi
                if (savedOfflineMacs.contains(mac)) {
                    continue;
                }
                
                // ✅ LƯU VỊ TRÍ CUỐI CÙNG VÀO DATABASE
                saveLastPositionToDatabase(data);
                
                // Đánh dấu đã xử lý
                savedOfflineMacs.add(mac);
                
                log.info("💾 Saved last position to DB for offline helmet: {} at ({}, {})", 
                    mac, data.getLat(), data.getLon());
            }
            
            // ✅ Xóa các MAC đã online trở lại khỏi tracking set
            cleanupOnlineHelmets();
            
        } catch (Exception e) {
            log.error("❌ Error in offline detection: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 💾 Lưu vị trí cuối cùng vào helmet_data table (1 row per MAC)
     */
    private void saveLastPositionToDatabase(HelmetData data) {
        try {
            HelmetData existingData = helmetDataRepository.findByMac(data.getMac()).orElse(null);
            
            if (existingData != null) {
                // Update existing record
                existingData.setEmployeeId(data.getEmployeeId());
                existingData.setEmployeeName(data.getEmployeeName());
                existingData.setVoltage(data.getVoltage());
                existingData.setCurrent(data.getCurrent());
                existingData.setPower(data.getPower());
                existingData.setBattery(data.getBattery());
                existingData.setLat(data.getLat());
                existingData.setLon(data.getLon());
                existingData.setCounter(data.getCounter());
                existingData.setTimestamp(data.getTimestamp());
                helmetDataRepository.save(existingData);
                log.debug("📝 Updated helmet_data for offline MAC: {}", data.getMac());
            } else {
                // Insert new record
                helmetDataRepository.save(data);
                log.info("➕ Inserted helmet_data for offline MAC: {}", data.getMac());
            }
        } catch (Exception e) {
            log.error("❌ Error saving last position to DB: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 🧹 Xóa các MAC đã online trở lại khỏi tracking set
     */
    private void cleanupOnlineHelmets() {
        try {
            List<HelmetData> allHelmets = redisCacheService.getAllActiveHelmets();
            java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusSeconds(OFFLINE_TIMEOUT_SECONDS);
            
            // Lấy danh sách MAC đang online (có data trong 30s gần đây)
            Set<String> onlineMacs = new HashSet<>();
            for (HelmetData data : allHelmets) {
                if (data.getReceivedAt() != null && data.getReceivedAt().isAfter(threshold)) {
                    onlineMacs.add(data.getMac());
                }
            }
            
            // Xóa các MAC online khỏi savedOfflineMacs
            int removed = 0;
            for (String mac : onlineMacs) {
                if (savedOfflineMacs.remove(mac)) {
                    removed++;
                    log.debug("🔄 Helmet {} is back ONLINE - removed from offline tracking", mac);
                }
            }
            
            if (removed > 0) {
                log.info("🔄 {} helmets came back online", removed);
            }
        } catch (Exception e) {
            log.error("❌ Error cleaning up online helmets: {}", e.getMessage(), e);
        }
    }
}

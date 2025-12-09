package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.TagLastPosition;
import com.hatrustsoft.bfe_foraiot.service.PositioningService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 📍 API Controller for Tag Positioning
 * Dùng cho positioning-2d.html
 * 🚀 OPTIMIZED: Cache 10s để giảm DB queries
 */
@RestController
@RequestMapping("/api/positioning")
@CrossOrigin(origins = "*")
@Slf4j
public class PositioningController {

    @Autowired
    private PositioningService positioningService;
    
    // ⏰ Đồng bộ với location.html: 30 giây không nhận data → offline
    private static final long OFFLINE_THRESHOLD_SECONDS = 30;
    
    /**
     * 📋 Lấy tất cả tag positions (online + offline)
     * Frontend dùng để hiển thị tags lúc load trang
     * ⏰ isOnline được tính realtime dựa trên lastSeen (30s threshold)
     * 🚀 CACHE: 10 giây để giảm DB queries từ 60-80 xuống 1
     */
    @GetMapping("/tags")
    @Cacheable(value = "tagPositions", unless = "#result == null")
    public ResponseEntity<List<TagPositionDTO>> getAllTagPositions() {
        List<TagLastPosition> tags = positioningService.getAllTagPositions();
        LocalDateTime now = LocalDateTime.now();
        
        List<TagPositionDTO> result = tags.stream()
            .map(tag -> toDTO(tag, now))
            .collect(Collectors.toList());
        
        log.info("📍 [CACHE MISS] Returning {} tag positions from DB", result.size());
        
        // Cache control header: cache 10s ở browser
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(10)))
            .body(result);
    }
    
    /**
     * ⚪ Lấy chỉ các offline tags
     * 🚀 CACHE: 10 giây
     */
    @GetMapping("/tags/offline")
    @Cacheable(value = "offlineTags", unless = "#result == null")
    public ResponseEntity<List<TagPositionDTO>> getOfflineTags() {
        List<TagLastPosition> tags = positioningService.getAllTagPositions();
        LocalDateTime now = LocalDateTime.now();
        
        List<TagPositionDTO> result = tags.stream()
            .map(tag -> toDTO(tag, now))
            .filter(dto -> !dto.getIsOnline()) // Chỉ lấy offline
            .collect(Collectors.toList());
        
        log.info("⚪ [CACHE MISS] Returning {} offline tags", result.size());
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(10)))
            .body(result);
    }
    
    /**
     * Convert entity to DTO
     * ⏰ Tính isOnline realtime: lastSeen trong 30s → online, ngược lại → offline
     */
    private TagPositionDTO toDTO(TagLastPosition entity, LocalDateTime now) {
        // ⏰ Tính toán isOnline dựa trên lastSeen (đồng bộ với location.html)
        boolean isOnline = false;
        if (entity.getLastSeen() != null) {
            long secondsAgo = ChronoUnit.SECONDS.between(entity.getLastSeen(), now);
            isOnline = secondsAgo <= OFFLINE_THRESHOLD_SECONDS;
            
            if (!isOnline) {
                log.debug("⚪ Tag {} offline: lastSeen {}s ago", entity.getMac(), secondsAgo);
            }
        }
        
        return TagPositionDTO.builder()
            .mac(entity.getMac())
            .employeeId(entity.getEmployeeId())
            .employeeName(entity.getEmployeeName())
            .x(entity.getLastX())
            .y(entity.getLastY())
            .distanceA0(entity.getDistanceA0())
            .distanceA1(entity.getDistanceA1())
            .distanceA2(entity.getDistanceA2())
            .battery(entity.getBattery())
            .isOnline(isOnline) // ⏰ Tính realtime, không lấy từ DB
            .lastSeen(entity.getLastSeen() != null ? entity.getLastSeen().toString() : null)
            .build();
    }
    
    /**
     * DTO cho tag position
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagPositionDTO {
        private String mac;
        private String employeeId;
        private String employeeName;
        private Double x;
        private Double y;
        private Double distanceA0;
        private Double distanceA1;
        private Double distanceA2;
        private Double battery;
        private Boolean isOnline;
        private String lastSeen;
    }
}

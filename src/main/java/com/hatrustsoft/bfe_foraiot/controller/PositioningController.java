package com.hatrustsoft.bfe_foraiot.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.TagLastPosition;
import com.hatrustsoft.bfe_foraiot.service.PositioningService;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 📍 API Controller for Tag Positioning
 * Dùng cho positioning-2d.html
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
     */
    @GetMapping("/tags")
    public ResponseEntity<List<TagPositionDTO>> getAllTagPositions() {
        List<TagLastPosition> tags = positioningService.getAllTagPositions();
        LocalDateTime now = VietnamTimeUtils.now(); // ✅ Fix timezone
        
        List<TagPositionDTO> result = tags.stream()
            .map(tag -> toDTO(tag, now))
            .collect(Collectors.toList());
        
        log.info("📍 Returning {} tag positions", result.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * ⚪ Lấy chỉ các offline tags
     */
    @GetMapping("/tags/offline")
    public ResponseEntity<List<TagPositionDTO>> getOfflineTags() {
        List<TagLastPosition> tags = positioningService.getAllTagPositions();
        LocalDateTime now = VietnamTimeUtils.now(); // ✅ Fix timezone
        
        List<TagPositionDTO> result = tags.stream()
            .map(tag -> toDTO(tag, now))
            .filter(dto -> !dto.getIsOnline()) // Chỉ lấy offline
            .collect(Collectors.toList());
        
        log.info("⚪ Returning {} offline tags", result.size());
        return ResponseEntity.ok(result);
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

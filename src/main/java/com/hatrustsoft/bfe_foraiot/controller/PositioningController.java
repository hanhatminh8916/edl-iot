package com.hatrustsoft.bfe_foraiot.controller;

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
    
    /**
     * 📋 Lấy tất cả tag positions (online + offline)
     * Frontend dùng để hiển thị tags lúc load trang
     */
    @GetMapping("/tags")
    public ResponseEntity<List<TagPositionDTO>> getAllTagPositions() {
        List<TagLastPosition> tags = positioningService.getAllTagPositions();
        
        List<TagPositionDTO> result = tags.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        log.info("📍 Returning {} tag positions", result.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * ⚪ Lấy chỉ các offline tags
     */
    @GetMapping("/tags/offline")
    public ResponseEntity<List<TagPositionDTO>> getOfflineTags() {
        List<TagLastPosition> tags = positioningService.getOfflineTags();
        
        List<TagPositionDTO> result = tags.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        log.info("⚪ Returning {} offline tags", result.size());
        return ResponseEntity.ok(result);
    }
    
    /**
     * Convert entity to DTO
     */
    private TagPositionDTO toDTO(TagLastPosition entity) {
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
            .isOnline(entity.getIsOnline())
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

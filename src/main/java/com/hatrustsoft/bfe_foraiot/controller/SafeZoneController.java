package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.entity.SafeZone;
import com.hatrustsoft.bfe_foraiot.repository.SafeZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/safe-zones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SafeZoneController {

    private final SafeZoneRepository safeZoneRepository;

    /**
     * Lấy khu vực an toàn active mới nhất
     */
    @GetMapping("/active")
    public ResponseEntity<SafeZone> getActiveSafeZone() {
        return safeZoneRepository.findLatestActiveZone()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Lấy tất cả khu vực an toàn
     */
    @GetMapping
    public ResponseEntity<List<SafeZone>> getAllSafeZones() {
        List<SafeZone> zones = safeZoneRepository.findAll();
        return ResponseEntity.ok(zones);
    }

    /**
     * Lưu khu vực an toàn mới (hoặc cập nhật nếu đã tồn tại)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveSafeZone(@RequestBody SaveZoneRequest request) {
        try {
            log.info("📍 Saving safe zone: name={}, coordinates={}", 
                request.getZoneName(), request.getPolygonCoordinates());

            // Tắt tất cả các zone cũ (chỉ giữ 1 zone active)
            List<SafeZone> activeZones = safeZoneRepository.findByIsActiveTrue();
            activeZones.forEach(zone -> {
                zone.setIsActive(false);
                safeZoneRepository.save(zone);
            });

            // Tạo zone mới
            SafeZone safeZone = new SafeZone();
            safeZone.setZoneName(request.getZoneName());
            safeZone.setPolygonCoordinates(request.getPolygonCoordinates());
            safeZone.setColor(request.getColor() != null ? request.getColor() : "#3388ff");
            safeZone.setIsActive(true);
            safeZone.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");

            SafeZone saved = safeZoneRepository.save(safeZone);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lưu khu vực an toàn thành công!");
            response.put("data", saved);

            log.info("✅ Safe zone saved: id={}, name={}", saved.getId(), saved.getZoneName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error saving safe zone", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Xóa khu vực an toàn
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSafeZone(@PathVariable Long id) {
        try {
            safeZoneRepository.deleteById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa khu vực thành công!");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting safe zone", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * DTO cho request lưu zone
     */
    @lombok.Data
    public static class SaveZoneRequest {
        private String zoneName;
        private String polygonCoordinates; // JSON string: [[lat,lon],[lat,lon],...]
        private String color;
        private String createdBy;
    }
}

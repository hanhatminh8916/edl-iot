package com.hatrustsoft.bfe_foraiot.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.model.Zone;
import com.hatrustsoft.bfe_foraiot.repository.ZoneRepository;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZoneController {

    private final ZoneRepository zoneRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<Zone>> getAllZones() {
        return ResponseEntity.ok(zoneRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Zone>> getActiveZones() {
        return ResponseEntity.ok(zoneRepository.findByStatusOrderByNameAsc(Zone.ZoneStatus.ACTIVE));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Zone> getZoneById(@PathVariable Long id) {
        return zoneRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ Kiểm tra tên khu vực đã tồn tại chưa
     */
    @GetMapping("/check-name")
    public ResponseEntity<Map<String, Object>> checkZoneName(@org.springframework.web.bind.annotation.RequestParam String name) {
        boolean exists = zoneRepository.existsByNameIgnoreCase(name.trim());
        Map<String, Object> result = new HashMap<>();
        result.put("exists", exists);
        result.put("name", name.trim());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping
    public ResponseEntity<?> createZone(@RequestBody Map<String, Object> payload) {
        String zoneName = (String) payload.get("name");
        
        // ✅ Kiểm tra trùng tên khu vực
        if (zoneName == null || zoneName.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Tên khu vực không được để trống");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (zoneRepository.existsByNameIgnoreCase(zoneName.trim())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Tên khu vực '" + zoneName.trim() + "' đã tồn tại. Vui lòng chọn tên khác.");
            return ResponseEntity.badRequest().body(error);
        }
        
        Zone zone = new Zone();
        zone.setName(zoneName.trim());
        zone.setDescription((String) payload.getOrDefault("description", ""));
        zone.setPolygonCoordinates((String) payload.get("polygonCoordinates"));
        zone.setColor((String) payload.getOrDefault("color", "#FFA500")); // Default orange/yellow
        zone.setStatus(Zone.ZoneStatus.ACTIVE);
        zone.setCreatedAt(VietnamTimeUtils.now());
        zone.setUpdatedAt(VietnamTimeUtils.now());

        Zone saved = zoneRepository.save(zone);
        
        // ✅ Broadcast CREATE event via WebSocket
        Map<String, Object> message = new HashMap<>();
        message.put("action", "CREATE");
        message.put("zone", saved);
        messagingTemplate.convertAndSend("/topic/zone/update", message);
        
        return ResponseEntity.created(URI.create("/api/zones/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Zone> updateZone(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return zoneRepository.findById(id)
                .map(zone -> {
                    if (payload.containsKey("name")) {
                        zone.setName((String) payload.get("name"));
                    }
                    if (payload.containsKey("description")) {
                        zone.setDescription((String) payload.get("description"));
                    }
                    if (payload.containsKey("polygonCoordinates")) {
                        zone.setPolygonCoordinates((String) payload.get("polygonCoordinates"));
                    }
                    if (payload.containsKey("color")) {
                        zone.setColor((String) payload.get("color"));
                    }
                    if (payload.containsKey("status")) {
                        zone.setStatus(Zone.ZoneStatus.valueOf((String) payload.get("status")));
                    }
                    zone.setUpdatedAt(VietnamTimeUtils.now());
                    
                    Zone updated = zoneRepository.save(zone);
                    
                    // ✅ Broadcast UPDATE event via WebSocket
                    Map<String, Object> message = new HashMap<>();
                    message.put("action", "UPDATE");
                    message.put("zone", updated);
                    messagingTemplate.convertAndSend("/topic/zone/update", message);
                    
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteZone(@PathVariable Long id) {
        return zoneRepository.findById(id)
                .map(zone -> {
                    zoneRepository.delete(zone);
                    
                    // ✅ Broadcast DELETE event via WebSocket
                    Map<String, Object> message = new HashMap<>();
                    message.put("action", "DELETE");
                    message.put("zoneId", id);
                    messagingTemplate.convertAndSend("/topic/zone/update", message);
                    
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Zone deleted successfully");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}



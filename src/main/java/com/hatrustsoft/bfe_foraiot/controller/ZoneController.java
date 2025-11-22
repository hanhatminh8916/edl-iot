package com.hatrustsoft.bfe_foraiot.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZoneController {

    private final ZoneRepository zoneRepository;

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

    @PostMapping
    public ResponseEntity<Zone> createZone(@RequestBody Map<String, Object> payload) {
        Zone zone = new Zone();
        zone.setName((String) payload.get("name"));
        zone.setDescription((String) payload.getOrDefault("description", ""));
        zone.setPolygonCoordinates((String) payload.get("polygonCoordinates"));
        zone.setColor((String) payload.getOrDefault("color", "#FFA500")); // Default orange/yellow
        zone.setStatus(Zone.ZoneStatus.ACTIVE);
        zone.setCreatedAt(LocalDateTime.now());
        zone.setUpdatedAt(LocalDateTime.now());

        Zone saved = zoneRepository.save(zone);
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
                    zone.setUpdatedAt(LocalDateTime.now());
                    
                    Zone updated = zoneRepository.save(zone);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteZone(@PathVariable Long id) {
        return zoneRepository.findById(id)
                .map(zone -> {
                    zoneRepository.delete(zone);
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Zone deleted successfully");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

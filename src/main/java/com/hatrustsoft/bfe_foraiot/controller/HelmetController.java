package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.dto.CommandDTO;
import com.hatrustsoft.bfe_foraiot.dto.HelmetDataDTO;
import com.hatrustsoft.bfe_foraiot.entity.HelmetData;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.service.HelmetService;
import com.hatrustsoft.bfe_foraiot.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/helmet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HelmetController {

    private final HelmetService helmetService;
    private final RedisCacheService redisCacheService;

    @PostMapping("/data")
    public ResponseEntity<Void> receiveData(@RequestBody HelmetDataDTO data) {
        helmetService.saveHelmetData(data);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<Helmet>> getActiveHelmets() {
        return ResponseEntity.ok(helmetService.getAllActiveHelmets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Helmet> getHelmet(@PathVariable Long id) {
        return ResponseEntity.ok(helmetService.getHelmetById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHelmetHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(helmetService.getHelmetHistory(id, hours));
    }

    @PostMapping("/{id}/command")
    public ResponseEntity<Void> sendCommand(
            @PathVariable Long id,
            @RequestBody CommandDTO command) {
        helmetService.sendCommandToHelmet(id, command);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get all helmets with realtime data from Redis (for management page)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllHelmets() {
        List<Helmet> helmets = helmetService.getAllHelmets();
        
        // Get realtime data from Redis
        List<HelmetData> cachedHelmets = redisCacheService.getAllActiveHelmets();
        Map<String, HelmetData> helmetDataByMac = cachedHelmets.stream()
                .collect(Collectors.toMap(HelmetData::getMac, h -> h, (a, b) -> a));
        
        // Enrich helmet data with realtime info
        List<Map<String, Object>> result = helmets.stream().map(helmet -> {
            Map<String, Object> helmetMap = new HashMap<>();
            helmetMap.put("id", helmet.getId());
            helmetMap.put("helmetId", helmet.getHelmetId());
            helmetMap.put("macAddress", helmet.getMacAddress());
            helmetMap.put("createdAt", helmet.getCreatedAt());
            helmetMap.put("worker", helmet.getEmployee()); // For frontend compatibility
            
            // Check Redis for realtime data
            HelmetData realtimeData = helmetDataByMac.get(helmet.getMacAddress());
            if (realtimeData != null) {
                // Use realtime battery from Redis
                helmetMap.put("batteryLevel", realtimeData.getBattery() != null ? 
                    realtimeData.getBattery().intValue() : 0);
                helmetMap.put("lastLat", realtimeData.getLat());
                helmetMap.put("lastLon", realtimeData.getLon());
                helmetMap.put("lastSeen", realtimeData.getReceivedAt());
                
                // Determine status based on receivedAt time
                LocalDateTime receivedAt = realtimeData.getReceivedAt();
                if (receivedAt != null) {
                    long minutesAgo = ChronoUnit.MINUTES.between(receivedAt, LocalDateTime.now());
                    if (minutesAgo <= 2) {
                        helmetMap.put("status", HelmetStatus.ACTIVE);
                    } else if (minutesAgo <= 10) {
                        helmetMap.put("status", HelmetStatus.INACTIVE);
                    } else {
                        helmetMap.put("status", HelmetStatus.OFFLINE);
                    }
                } else {
                    helmetMap.put("status", helmet.getStatus());
                }
            } else {
                // Fallback to database values
                helmetMap.put("batteryLevel", helmet.getBatteryLevel());
                helmetMap.put("lastLat", helmet.getLastLat());
                helmetMap.put("lastLon", helmet.getLastLon());
                helmetMap.put("lastSeen", helmet.getLastSeen());
                helmetMap.put("status", helmet.getStatus());
            }
            
            return helmetMap;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Assign helmet to worker
     */
    @PostMapping("/{helmetId}/assign/{workerId}")
    public ResponseEntity<Helmet> assignToWorker(
            @PathVariable Long helmetId,
            @PathVariable Long workerId) {
        return ResponseEntity.ok(helmetService.assignHelmetToWorker(helmetId, workerId));
    }
}

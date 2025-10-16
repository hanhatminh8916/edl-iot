package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/pending")
    public ResponseEntity<List<Alert>> getPendingAlerts() {
        return ResponseEntity.ok(alertService.getPendingAlerts());
    }

    @GetMapping("/today")
    public ResponseEntity<List<Alert>> getTodayAlerts() {
        return ResponseEntity.ok(alertService.getTodayAlerts());
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable Long id,
            @RequestParam String username) {
        alertService.acknowledgeAlert(id, username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long id) {
        alertService.resolveAlert(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(alertService.getStatistics(days));
    }
}

package com.hatrustsoft.bfe_foraiot.controller;

import com.hatrustsoft.bfe_foraiot.dto.CommandDTO;
import com.hatrustsoft.bfe_foraiot.dto.HelmetDataDTO;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.service.HelmetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/helmet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HelmetController {

    private final HelmetService helmetService;

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
}

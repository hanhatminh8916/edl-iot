package com.hatrustsoft.bfe_foraiot.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.model.Worker;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.repository.WorkerRepository;
import com.hatrustsoft.bfe_foraiot.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkerController {

    private final WorkerRepository workerRepository;
    private final HelmetRepository helmetRepository;
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listWorkers() {
        return ResponseEntity.ok(dashboardService.getWorkersList(null));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorker(@RequestBody Map<String, Object> payload) {
        Worker w = new Worker();
        w.setFullName((String) payload.getOrDefault("name", payload.getOrDefault("fullName", "")));
        w.setEmployeeId((String) payload.getOrDefault("employeeId", ""));
        w.setPosition((String) payload.getOrDefault("position", ""));
        w.setDepartment((String) payload.getOrDefault("department", ""));
        w.setPhoneNumber((String) payload.getOrDefault("phone", ""));
        w.setStatus(Worker.WorkerStatus.ACTIVE); // Set default status
        w.setCreatedAt(LocalDateTime.now());
        w.setUpdatedAt(LocalDateTime.now());

        Worker saved = workerRepository.save(w);

        // Assign helmet if provided
        if (payload.containsKey("helmetId")) {
            try {
                Long helmetId = Long.valueOf(payload.get("helmetId").toString());
                helmetRepository.findById(helmetId).ifPresent(helmet -> {
                    helmet.setWorker(saved);
                    helmet.setUpdatedAt(LocalDateTime.now());
                    helmetRepository.save(helmet);
                });
            } catch (NumberFormatException e) {
                // Invalid helmetId, ignore
            }
        }

        // return the newly created worker structure similar to GET
        return ResponseEntity.created(URI.create("/api/workers/" + saved.getId()))
                .body(Map.of("id", saved.getId(), "name", saved.getFullName(), "employeeId", saved.getEmployeeId()));
    }
}

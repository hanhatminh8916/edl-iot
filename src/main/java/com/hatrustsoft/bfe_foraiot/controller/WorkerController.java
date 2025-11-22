package com.hatrustsoft.bfe_foraiot.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.model.Worker;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listWorkers() {
        return ResponseEntity.ok(dashboardService.getWorkersList(null));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorker(@RequestBody Map<String, Object> payload) {
        // Auto-generate employee ID in format REV01-REVn
        String employeeId = generateNextEmployeeId();
        
        Worker w = new Worker();
        w.setFullName((String) payload.getOrDefault("name", payload.getOrDefault("fullName", "")));
        w.setEmployeeId(employeeId);
        w.setPosition((String) payload.getOrDefault("position", ""));
        w.setLocation((String) payload.getOrDefault("location", ""));
        w.setDepartment((String) payload.getOrDefault("department", ""));
        w.setPhoneNumber((String) payload.getOrDefault("phone", ""));
        w.setStatus(Worker.WorkerStatus.ACTIVE); // Set default status
        w.setCreatedAt(LocalDateTime.now());
        w.setUpdatedAt(LocalDateTime.now());

        Worker saved = workerRepository.save(w);

        // ⭐ Sync to Employee table
        Employee employee = new Employee();
        employee.setEmployeeId(saved.getEmployeeId());
        employee.setName(saved.getFullName());
        employee.setPosition(saved.getPosition());
        employee.setDepartment(saved.getDepartment());
        employee.setPhoneNumber(saved.getPhoneNumber());
        employee.setStatus("ACTIVE");
        
        // Assign helmet if provided
        if (payload.containsKey("helmetId")) {
            try {
                Long helmetId = Long.valueOf(payload.get("helmetId").toString());
                helmetRepository.findById(helmetId).ifPresent(helmet -> {
                    helmet.setWorker(saved);
                    helmet.setUpdatedAt(LocalDateTime.now());
                    helmetRepository.save(helmet);
                    
                    // ⭐ Update Employee with macAddress from helmet
                    if (helmet.getMacAddress() != null) {
                        employee.setMacAddress(helmet.getMacAddress());
                    }
                });
            } catch (NumberFormatException e) {
                // Invalid helmetId, ignore
            }
        }
        
        // ⭐ Save Employee
        employeeRepository.save(employee);

        // return the newly created worker structure similar to GET
        return ResponseEntity.created(URI.create("/api/workers/" + saved.getId()))
                .body(Map.of("id", saved.getId(), "name", saved.getFullName(), "employeeId", saved.getEmployeeId()));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorker(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> payload) {
        
        return workerRepository.findById(id)
            .map(worker -> {
                // Update basic fields if provided
                if (payload.containsKey("name")) {
                    worker.setFullName((String) payload.get("name"));
                }
                if (payload.containsKey("phone")) {
                    worker.setPhoneNumber((String) payload.get("phone"));
                }
                if (payload.containsKey("position")) {
                    worker.setPosition((String) payload.get("position"));
                }
                if (payload.containsKey("location")) {
                    worker.setLocation((String) payload.get("location"));
                }
                if (payload.containsKey("department")) {
                    worker.setDepartment((String) payload.get("department"));
                }
                
                worker.setUpdatedAt(LocalDateTime.now());
                Worker updated = workerRepository.save(worker);
                
                // ⭐ Sync to Employee table
                Employee employee = employeeRepository.findById(updated.getEmployeeId())
                    .orElse(new Employee());
                employee.setEmployeeId(updated.getEmployeeId());
                employee.setName(updated.getFullName());
                employee.setPosition(updated.getPosition());
                employee.setDepartment(updated.getDepartment());
                employee.setPhoneNumber(updated.getPhoneNumber());
                employee.setStatus(updated.getStatus().toString());
                
                // Handle helmet assignment/reassignment
                if (payload.containsKey("helmetId")) {
                    try {
                        Long newHelmetId = Long.valueOf(payload.get("helmetId").toString());
                        
                        // Remove worker from old helmet (if any)
                        helmetRepository.findByWorker(worker).ifPresent(oldHelmet -> {
                            oldHelmet.setWorker(null);
                            oldHelmet.setUpdatedAt(LocalDateTime.now());
                            helmetRepository.save(oldHelmet);
                            
                            // ⭐ Clear macAddress from old employee
                            if (oldHelmet.getMacAddress() != null) {
                                employee.setMacAddress(null);
                            }
                        });
                        
                        // Assign worker to new helmet
                        helmetRepository.findById(newHelmetId).ifPresent(newHelmet -> {
                            newHelmet.setWorker(updated);
                            newHelmet.setUpdatedAt(LocalDateTime.now());
                            helmetRepository.save(newHelmet);
                            
                            // ⭐ Update Employee with macAddress from new helmet
                            if (newHelmet.getMacAddress() != null) {
                                employee.setMacAddress(newHelmet.getMacAddress());
                            }
                        });
                    } catch (NumberFormatException e) {
                        // Invalid helmetId, ignore
                    }
                }
                
                // ⭐ Save Employee
                employeeRepository.save(employee);
                
                return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "name", updated.getFullName(),
                    "employeeId", updated.getEmployeeId(),
                    "message", "Worker updated successfully"
                ));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    // Generate next employee ID in format REV01, REV02, ..., REVn
    private String generateNextEmployeeId() {
        List<Worker> allWorkers = workerRepository.findAll();
        
        // Find the highest number from existing REVxx IDs
        int maxNumber = allWorkers.stream()
            .map(Worker::getEmployeeId)
            .filter(id -> id != null && id.matches("REV\\d+"))
            .map(id -> id.substring(3)) // Remove "REV" prefix
            .map(num -> {
                try {
                    return Integer.parseInt(num);
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max(Integer::compareTo)
            .orElse(0);
        
        // Generate next ID
        int nextNumber = maxNumber + 1;
        return String.format("REV%02d", nextNumber);
    }
}

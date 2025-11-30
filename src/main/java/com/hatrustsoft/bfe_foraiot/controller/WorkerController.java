package com.hatrustsoft.bfe_foraiot.controller;

import java.net.URI;
import java.util.HashMap;
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
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.service.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class WorkerController {

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
        
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setName((String) payload.getOrDefault("name", payload.getOrDefault("fullName", "")));
        employee.setPosition((String) payload.getOrDefault("position", ""));
        employee.setLocation((String) payload.getOrDefault("location", ""));
        employee.setDepartment((String) payload.getOrDefault("department", ""));
        employee.setPhoneNumber((String) payload.getOrDefault("phone", ""));
        employee.setEmail((String) payload.getOrDefault("email", ""));
        employee.setStatus("ACTIVE");
        
        // Assign helmet if provided
        if (payload.containsKey("helmetId")) {
            try {
                Long helmetId = Long.valueOf(payload.get("helmetId").toString());
                helmetRepository.findById(helmetId).ifPresent(helmet -> {
                    // Update Employee with macAddress from helmet
                    if (helmet.getMacAddress() != null) {
                        employee.setMacAddress(helmet.getMacAddress());
                    }
                });
            } catch (NumberFormatException e) {
                log.warn("Invalid helmetId: {}", payload.get("helmetId"));
            }
        }
        
        // Save Employee first
        Employee saved = employeeRepository.save(employee);
        
        // Now assign helmet to employee if provided
        if (payload.containsKey("helmetId")) {
            try {
                Long helmetId = Long.valueOf(payload.get("helmetId").toString());
                helmetRepository.findById(helmetId).ifPresent(helmet -> {
                    helmet.setEmployee(saved);
                    helmetRepository.save(helmet);
                });
            } catch (NumberFormatException e) {
                log.warn("Invalid helmetId: {}", payload.get("helmetId"));
            }
        }

        log.info("✅ Created employee: {} ({})", saved.getName(), saved.getEmployeeId());
        
        return ResponseEntity.created(URI.create("/api/workers/" + saved.getId()))
                .body(Map.of("id", saved.getId(), "name", saved.getName(), "employeeId", saved.getEmployeeId()));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorker(
            @PathVariable Long id, 
            @RequestBody Map<String, Object> payload) {
        
        return employeeRepository.findById(id)
            .map(employee -> {
                // Update basic fields if provided
                if (payload.containsKey("name")) {
                    employee.setName((String) payload.get("name"));
                }
                if (payload.containsKey("phone")) {
                    employee.setPhoneNumber((String) payload.get("phone"));
                }
                if (payload.containsKey("position")) {
                    employee.setPosition((String) payload.get("position"));
                }
                if (payload.containsKey("location")) {
                    employee.setLocation((String) payload.get("location"));
                }
                if (payload.containsKey("department")) {
                    employee.setDepartment((String) payload.get("department"));
                }
                if (payload.containsKey("email")) {
                    employee.setEmail((String) payload.get("email"));
                }
                if (payload.containsKey("status")) {
                    employee.setStatus((String) payload.get("status"));
                }
                
                // Handle helmet assignment/reassignment
                if (payload.containsKey("helmetId")) {
                    try {
                        Long newHelmetId = Long.valueOf(payload.get("helmetId").toString());
                        
                        // Remove employee from old helmet (if any)
                        helmetRepository.findByEmployee(employee).ifPresent(oldHelmet -> {
                            oldHelmet.setEmployee(null);
                            helmetRepository.save(oldHelmet);
                            
                            // Clear macAddress from employee
                            employee.setMacAddress(null);
                        });
                        
                        // Assign employee to new helmet
                        helmetRepository.findById(newHelmetId).ifPresent(newHelmet -> {
                            newHelmet.setEmployee(employee);
                            helmetRepository.save(newHelmet);
                            
                            // Update Employee with macAddress from new helmet
                            if (newHelmet.getMacAddress() != null) {
                                employee.setMacAddress(newHelmet.getMacAddress());
                            }
                        });
                    } catch (NumberFormatException e) {
                        log.warn("Invalid helmetId: {}", payload.get("helmetId"));
                    }
                }
                
                Employee updated = employeeRepository.save(employee);
                log.info("✅ Updated employee: {} ({})", updated.getName(), updated.getEmployeeId());
                
                return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "name", updated.getName(),
                    "employeeId", updated.getEmployeeId(),
                    "message", "Worker updated successfully"
                ));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorker(@PathVariable Long id) {
        return employeeRepository.findById(id)
            .map(employee -> {
                Map<String, Object> result = new HashMap<>();
                result.put("id", employee.getId());
                result.put("name", employee.getName());
                result.put("employeeId", employee.getEmployeeId());
                result.put("position", employee.getPosition());
                result.put("department", employee.getDepartment());
                result.put("location", employee.getLocation());
                result.put("phone", employee.getPhoneNumber());
                result.put("email", employee.getEmail());
                result.put("status", employee.getStatus());
                result.put("macAddress", employee.getMacAddress());
                
                // Find helmet assigned to this employee
                helmetRepository.findByEmployee(employee).ifPresent(helmet -> {
                    Map<String, Object> helmetData = new HashMap<>();
                    helmetData.put("id", helmet.getId());
                    helmetData.put("helmetId", "HELMET-" + String.format("%03d", helmet.getHelmetId()));
                    helmetData.put("macAddress", helmet.getMacAddress());
                    helmetData.put("batteryLevel", helmet.getBatteryLevel());
                    helmetData.put("status", helmet.getStatus());
                    result.put("helmet", helmetData);
                });
                
                return ResponseEntity.ok(result);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    // Generate next employee ID in format REV01, REV02, ..., REVn
    private String generateNextEmployeeId() {
        List<Employee> allEmployees = employeeRepository.findAll();
        
        // Find the highest number from existing REVxx IDs
        int maxNumber = allEmployees.stream()
            .map(Employee::getEmployeeId)
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

package com.hatrustsoft.bfe_foraiot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Lấy danh sách tất cả nhân viên
     */
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return ResponseEntity.ok(employees);
    }

    /**
     * Lấy thông tin nhân viên theo ID (Long)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployee(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lấy thông tin nhân viên theo Employee ID (REV01, REV02, ...)
     */
    @GetMapping("/by-employee-id/{employeeId}")
    public ResponseEntity<?> getEmployeeByEmployeeId(@PathVariable String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tạo nhân viên mới
     */
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {
        try {
            // Kiểm tra MAC address trùng
            if (employee.getMacAddress() != null && 
                employeeRepository.existsByMacAddress(employee.getMacAddress())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "MAC address already assigned to another employee"));
            }

            Employee saved = employeeRepository.save(employee);
            log.info("✅ Created employee: {} with MAC: {}", saved.getEmployeeId(), saved.getMacAddress());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("❌ Error creating employee: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật thông tin nhân viên
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee employee) {
        try {
            return employeeRepository.findById(id)
                    .map(existing -> {
                        // Kiểm tra MAC address trùng (nếu thay đổi)
                        if (employee.getMacAddress() != null && 
                            !employee.getMacAddress().equals(existing.getMacAddress()) &&
                            employeeRepository.existsByMacAddress(employee.getMacAddress())) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", "MAC address already assigned to another employee"));
                        }

                        existing.setName(employee.getName());
                        existing.setPosition(employee.getPosition());
                        existing.setDepartment(employee.getDepartment());
                        existing.setLocation(employee.getLocation());
                        existing.setMacAddress(employee.getMacAddress());
                        existing.setPhoneNumber(employee.getPhoneNumber());
                        existing.setEmail(employee.getEmail());
                        existing.setStatus(employee.getStatus());

                        Employee updated = employeeRepository.save(existing);
                        log.info("✅ Updated employee: {} with MAC: {}", updated.getEmployeeId(), updated.getMacAddress());
                        return ResponseEntity.ok(updated);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("❌ Error updating employee: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xóa nhân viên
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            if (employeeRepository.existsById(id)) {
                employeeRepository.deleteById(id);
                log.info("✅ Deleted employee with id: {}", id);
                return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ Error deleting employee: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Gán MAC address cho nhân viên
     */
    @PutMapping("/{id}/assign-mac")
    public ResponseEntity<?> assignMacAddress(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String macAddress = payload.get("macAddress");
            
            if (macAddress == null || macAddress.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "MAC address is required"));
            }

            return employeeRepository.findById(id)
                    .map(employee -> {
                        // Kiểm tra MAC đã được gán cho người khác chưa
                        if (employeeRepository.existsByMacAddress(macAddress)) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", "MAC address already assigned to another employee"));
                        }

                        employee.setMacAddress(macAddress);
                        Employee updated = employeeRepository.save(employee);
                        log.info("✅ Assigned MAC {} to employee {}", macAddress, employee.getEmployeeId());
                        return ResponseEntity.ok(updated);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("❌ Error assigning MAC: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}

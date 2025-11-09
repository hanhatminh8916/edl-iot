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
     * Lấy thông tin nhân viên theo ID
     */
    @GetMapping("/{employeeId}")
    public ResponseEntity<?> getEmployee(@PathVariable String employeeId) {
        return employeeRepository.findById(employeeId)
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
    @PutMapping("/{employeeId}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable String employeeId,
            @RequestBody Employee employee) {
        try {
            return employeeRepository.findById(employeeId)
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
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<?> deleteEmployee(@PathVariable String employeeId) {
        try {
            if (employeeRepository.existsById(employeeId)) {
                employeeRepository.deleteById(employeeId);
                log.info("✅ Deleted employee: {}", employeeId);
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
    @PutMapping("/{employeeId}/assign-mac")
    public ResponseEntity<?> assignMacAddress(
            @PathVariable String employeeId,
            @RequestBody Map<String, String> payload) {
        try {
            String macAddress = payload.get("macAddress");
            
            if (macAddress == null || macAddress.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "MAC address is required"));
            }

            return employeeRepository.findById(employeeId)
                    .map(employee -> {
                        // Kiểm tra MAC đã được gán cho người khác chưa
                        if (employeeRepository.existsByMacAddress(macAddress)) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", "MAC address already assigned to another employee"));
                        }

                        employee.setMacAddress(macAddress);
                        Employee updated = employeeRepository.save(employee);
                        log.info("✅ Assigned MAC {} to employee {}", macAddress, employeeId);
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

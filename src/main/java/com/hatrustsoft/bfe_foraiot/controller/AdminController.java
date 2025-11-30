package com.hatrustsoft.bfe_foraiot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.config.DataInitializer;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final EmployeeRepository employeeRepository;
    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final HelmetDataRepository helmetDataRepository;
    
    @Autowired(required = false) // Optional: Có thể null nếu DataInitializer bị disable
    private DataInitializer dataInitializer;

    @PostMapping("/reset-data")
    public ResponseEntity<String> resetData() {
        try {
            // Kiểm tra xem DataInitializer có available không
            if (dataInitializer == null) {
                return ResponseEntity.badRequest()
                    .body("Tính năng reset-data đã bị tắt trên Heroku để tiết kiệm database queries. " +
                          "Vui lòng tạo dữ liệu thủ công qua API.");
            }
            
            // Delete all data
            helmetDataRepository.deleteAll();
            alertRepository.deleteAll();
            helmetRepository.deleteAll();
            employeeRepository.deleteAll();
            
            // Re-initialize sample data
            dataInitializer.run();
            
            return ResponseEntity.ok("Dữ liệu đã được reset thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Lỗi khi reset dữ liệu: " + e.getMessage());
        }
    }
}

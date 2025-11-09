package com.hatrustsoft.bfe_foraiot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    /**
     * Tìm nhân viên theo MAC address của helmet
     */
    Optional<Employee> findByMacAddress(String macAddress);
    
    /**
     * Kiểm tra MAC address đã được gán cho nhân viên nào chưa
     */
    boolean existsByMacAddress(String macAddress);
}

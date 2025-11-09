package com.hatrustsoft.bfe_foraiot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.entity.HelmetData;

@Repository
public interface HelmetDataRepository extends JpaRepository<HelmetData, Long> {
    
    // Tìm dữ liệu theo MAC address
    List<HelmetData> findByMacOrderByTimestampDesc(String mac);
    
    // Lấy bản ghi mới nhất theo MAC
    Optional<HelmetData> findFirstByMacOrderByTimestampDesc(String mac);
    
    // Tìm theo employeeId
    List<HelmetData> findByEmployeeIdOrderByTimestampDesc(String employeeId);
    
    // Lấy dữ liệu trong khoảng thời gian
    List<HelmetData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    // Lấy 10 bản ghi mới nhất
    List<HelmetData> findTop10ByOrderByTimestampDesc();
    
    // Tìm dữ liệu với battery thấp
    List<HelmetData> findByBatteryLessThanOrderByTimestampDesc(Double batteryLevel);
}

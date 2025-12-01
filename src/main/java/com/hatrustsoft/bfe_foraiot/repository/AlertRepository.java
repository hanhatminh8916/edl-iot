package com.hatrustsoft.bfe_foraiot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.AlertType;
import com.hatrustsoft.bfe_foraiot.model.Helmet;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatus(AlertStatus status);
    List<Alert> findByTriggeredAtAfter(LocalDateTime dateTime);
    
    // 🚀 Tìm alert theo helmet và type (để upsert - mỗi helmet chỉ có 1 alert/type)
    Optional<Alert> findByHelmetAndAlertType(Helmet helmet, AlertType alertType);
    
    // 🚀 Tìm tất cả PENDING alerts (để hiển thị radar khi load trang)
    List<Alert> findByStatusOrderByTriggeredAtDesc(AlertStatus status);
    
    // 🚀 Tìm PENDING alerts cho 1 helmet cụ thể
    List<Alert> findByHelmetAndStatus(Helmet helmet, AlertStatus status);
}

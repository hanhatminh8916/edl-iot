package com.hatrustsoft.bfe_foraiot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    // 🚀 TỐI ƯU: Đếm alerts sau thời điểm (thay vì lấy toàn bộ list rồi đếm)
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.triggeredAt > :dateTime")
    long countByTriggeredAtAfter(@Param("dateTime") LocalDateTime dateTime);
    
    // 🚀 TỐI ƯU: Lấy alerts với JOIN FETCH để tránh N+1
    @Query("SELECT a FROM Alert a LEFT JOIN FETCH a.helmet h LEFT JOIN FETCH h.employee WHERE a.triggeredAt > :dateTime ORDER BY a.triggeredAt DESC")
    List<Alert> findAlertsWithDetailsAfter(@Param("dateTime") LocalDateTime dateTime);
    
    // 🚀 TỐI ƯU: Đếm PENDING alerts
    long countByStatus(AlertStatus status);
    
    // 🚀 TỐI ƯU: Lấy 5 alerts mới nhất (có index)
    @Query("SELECT a FROM Alert a WHERE a.triggeredAt > :dateTime ORDER BY a.triggeredAt DESC")
    List<Alert> findTop5ByTriggeredAtAfterOrderByTriggeredAtDesc(@Param("dateTime") LocalDateTime dateTime);
}

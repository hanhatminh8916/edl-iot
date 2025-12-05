package com.hatrustsoft.bfe_foraiot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.entity.PushSubscription;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    
    /**
     * Tìm subscription theo endpoint
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);
    
    /**
     * Kiểm tra endpoint đã tồn tại chưa
     */
    boolean existsByEndpoint(String endpoint);
    
    /**
     * Lấy tất cả subscriptions đang active
     */
    List<PushSubscription> findByIsActiveTrue();
    
    /**
     * Xóa subscription theo endpoint
     */
    @Modifying
    @Query("DELETE FROM PushSubscription p WHERE p.endpoint = :endpoint")
    void deleteByEndpoint(String endpoint);
    
    /**
     * Deactivate subscription theo endpoint (soft delete)
     */
    @Modifying
    @Query("UPDATE PushSubscription p SET p.isActive = false WHERE p.endpoint = :endpoint")
    void deactivateByEndpoint(String endpoint);
    
    /**
     * Đếm số subscriptions active
     */
    long countByIsActiveTrue();
}

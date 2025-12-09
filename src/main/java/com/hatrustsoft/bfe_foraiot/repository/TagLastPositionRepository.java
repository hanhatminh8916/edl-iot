package com.hatrustsoft.bfe_foraiot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.entity.TagLastPosition;

/**
 * 🚀 OPTIMIZED: Cache findAll() để giảm queries từ 60-80 xuống gần 0
 */
@Repository
public interface TagLastPositionRepository extends JpaRepository<TagLastPosition, Long> {
    
    // Tìm theo MAC address
    Optional<TagLastPosition> findByMac(String mac);
    
    // Lấy tất cả tag online
    List<TagLastPosition> findByIsOnlineTrue();
    
    // Lấy tất cả tag offline
    List<TagLastPosition> findByIsOnlineFalse();
    
    // 🚀 CACHE: Lấy tất cả tags (cả online và offline) - cache 10s
    @Override
    @Cacheable(value = "allTags", unless = "#result == null || #result.isEmpty()")
    List<TagLastPosition> findAll();
    
    // Đánh dấu offline các tag không hoạt động trong khoảng thời gian
    @Modifying
    @Query("UPDATE TagLastPosition t SET t.isOnline = false WHERE t.lastSeen < :threshold AND t.isOnline = true")
    int markOfflineByLastSeenBefore(@Param("threshold") LocalDateTime threshold);
    
    // Đếm số tag online
    long countByIsOnlineTrue();
    
    // Đếm số tag offline
    long countByIsOnlineFalse();
    
    // Xóa các tag cũ không hoạt động trong thời gian dài (cleanup)
    @Modifying
    @Query("DELETE FROM TagLastPosition t WHERE t.lastSeen < :threshold")
    int deleteByLastSeenBefore(@Param("threshold") LocalDateTime threshold);
}

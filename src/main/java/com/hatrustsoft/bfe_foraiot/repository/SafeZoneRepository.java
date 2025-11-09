package com.hatrustsoft.bfe_foraiot.repository;

import com.hatrustsoft.bfe_foraiot.entity.SafeZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafeZoneRepository extends JpaRepository<SafeZone, Long> {

    // Tìm khu vực theo tên
    Optional<SafeZone> findByZoneName(String zoneName);

    // Lấy tất cả khu vực active
    List<SafeZone> findByIsActiveTrue();

    // Lấy khu vực active mới nhất
    @Query("SELECT s FROM SafeZone s WHERE s.isActive = true ORDER BY s.updatedAt DESC")
    Optional<SafeZone> findLatestActiveZone();
}

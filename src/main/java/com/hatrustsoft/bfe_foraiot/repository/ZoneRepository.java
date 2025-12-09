package com.hatrustsoft.bfe_foraiot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.model.Zone;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByStatusOrderByNameAsc(Zone.ZoneStatus status);
    List<Zone> findAllByOrderByNameAsc();
    
    // ✅ Kiểm tra trùng tên khu vực
    Optional<Zone> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}

package com.hatrustsoft.bfe_foraiot.repository;

import com.hatrustsoft.bfe_foraiot.entity.Anchor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnchorRepository extends JpaRepository<Anchor, Long> {
    
    Optional<Anchor> findByAnchorId(String anchorId);
    
    List<Anchor> findByStatus(String status);
    
    boolean existsByAnchorId(String anchorId);
}

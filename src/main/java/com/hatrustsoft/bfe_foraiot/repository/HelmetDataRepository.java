package com.hatrustsoft.bfe_foraiot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.model.HelmetData;

@Repository
public interface HelmetDataRepository extends JpaRepository<HelmetData, Long> {
}

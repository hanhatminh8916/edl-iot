package com.hatrustsoft.bfe_foraiot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;

@Repository
public interface HelmetRepository extends JpaRepository<Helmet, Long> {
    List<Helmet> findByStatus(HelmetStatus status);
    Optional<Helmet> findByHelmetId(Long helmetId);
    Optional<Helmet> findByMacAddress(String macAddress);
    List<Helmet> findAllByOrderByHelmetIdAsc();
    Optional<Helmet> findByEmployee(Employee employee);
}

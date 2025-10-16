package com.hatrustsoft.bfe_foraiot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.model.Worker;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
}

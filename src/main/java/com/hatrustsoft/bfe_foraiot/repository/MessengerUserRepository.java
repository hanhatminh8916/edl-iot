package com.hatrustsoft.bfe_foraiot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hatrustsoft.bfe_foraiot.entity.MessengerUser;

@Repository
public interface MessengerUserRepository extends JpaRepository<MessengerUser, Long> {
    
    Optional<MessengerUser> findByPsid(String psid);
    
    Optional<MessengerUser> findByEmployeeId(String employeeId);
    
    List<MessengerUser> findBySubscribedTrue();
}

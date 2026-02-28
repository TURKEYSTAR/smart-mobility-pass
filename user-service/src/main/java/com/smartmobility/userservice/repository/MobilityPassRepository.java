package com.smartmobility.userservice.repository;

import com.smartmobility.userservice.entity.MobilityPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobilityPassRepository extends JpaRepository<MobilityPass, Long> {

    Optional<MobilityPass> findByPassNumber(String passNumber);
}
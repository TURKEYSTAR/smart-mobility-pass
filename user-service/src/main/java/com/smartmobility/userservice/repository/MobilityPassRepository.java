package com.smartmobility.userservice.repository;

import com.smartmobility.userservice.entity.MobilityPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MobilityPassRepository extends JpaRepository<MobilityPass, UUID> {

    Optional<MobilityPass> findByPassNumber(String passNumber);
}
package com.smartmobility.pricingservice.repository;

import com.smartmobility.pricingservice.entity.FareCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FareCalculationRepository extends JpaRepository<FareCalculation, UUID> {
    Optional<FareCalculation> findByTripId(UUID tripId);
}

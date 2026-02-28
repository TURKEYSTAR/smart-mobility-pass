package com.smartmobility.pricingservice.repository;

import com.smartmobility.pricingservice.entity.PricingRule;
import com.smartmobility.pricingservice.entity.TransportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {
    Optional<PricingRule> findByTransportTypeAndActiveTrue(TransportType transportType);
}

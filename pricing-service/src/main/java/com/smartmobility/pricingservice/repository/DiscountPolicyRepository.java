package com.smartmobility.pricingservice.repository;

import com.smartmobility.pricingservice.entity.DiscountPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountPolicyRepository extends JpaRepository<DiscountPolicy, UUID> {
    List<DiscountPolicy> findByActiveTrue();
    List<DiscountPolicy> findByApplicationTierOrApplicationTierIsNull(String tier);
}

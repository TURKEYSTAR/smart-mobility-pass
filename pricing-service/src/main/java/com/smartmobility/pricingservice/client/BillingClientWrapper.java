package com.smartmobility.pricingservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wrapper du BillingServiceClient avec Circuit Breaker.
 *
 * ⚠️  Le @CircuitBreaker NE FONCTIONNE PAS si appelé depuis la même classe
 *     (self-invocation → Spring AOP bypassed).
 *     Solution : ce composant séparé est injecté dans FareCalculatorService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BillingClientWrapper {

    private final BillingServiceClient billingServiceClient;

    @CircuitBreaker(name = "billing-service", fallbackMethod = "getDailyTotalFallback")
    public BigDecimal getDailyTotal(UUID passId) {
        return billingServiceClient.getDailyTotal(passId);
    }

    public BigDecimal getDailyTotalFallback(UUID passId, Throwable t) {
        log.warn("[BillingWrapper] Circuit Breaker ouvert pour billing-service: {}", t.getMessage());
        return null; // null = plafond non vérifié, calcul continue sans lui
    }
}

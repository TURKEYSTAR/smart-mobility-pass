package com.smartmobility.tripservice.client;

import com.smartmobility.tripservice.dto.FareResultDTO;
import com.smartmobility.tripservice.dto.PricingRequest;
import com.smartmobility.tripservice.entity.TransportType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * PricingClientWrapper — Circuit Breaker dans un bean séparé.
 * ⚠️ @CircuitBreaker ne fonctionne pas en self-invocation (même classe).
 *    Ce wrapper est injecté dans TripService pour que Spring AOP puisse l'intercepter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PricingClientWrapper {

    private final PricingServiceClient pricingServiceClient;

    private static final Map<TransportType, BigDecimal> FALLBACK_FARES = Map.of(
            TransportType.BUS_CLASSIQUE, BigDecimal.valueOf(200),
            TransportType.BRT,           BigDecimal.valueOf(350),
            TransportType.TER,           BigDecimal.valueOf(500)
    );

    @CircuitBreaker(name = "pricing-service", fallbackMethod = "pricingFallback")
    public FareResultDTO calculateFare(PricingRequest request) {
        return pricingServiceClient.calculateFare(request);
    }

    public FareResultDTO pricingFallback(PricingRequest request, Throwable t) {
        log.warn("[PricingWrapper] Circuit Breaker ouvert : {}", t.getMessage());

        BigDecimal fallbackAmount = FALLBACK_FARES.getOrDefault(
                request.getTransportType(), BigDecimal.valueOf(200)
        );

        log.warn("[PricingWrapper] Fallback : {} FCFA pour {}", fallbackAmount, request.getTransportType());

        return FareResultDTO.builder()
                .baseAmount(fallbackAmount)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(fallbackAmount)
                .appliedDiscounts(Collections.emptyList())
                .cappedByDailyLimit(false)
                .fallbackUsed(true)
                .note("Tarif standard appliqué (Pricing Service indisponible)")
                .build();
    }
}

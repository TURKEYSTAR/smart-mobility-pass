package com.smartmobility.tripservice.messaging;

import com.smartmobility.tripservice.entity.TransportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing-key.trip-completed}")
    private String tripCompletedKey;

    @Value("${rabbitmq.routing-key.pricing-fallback}")
    private String pricingFallbackKey;

    @Value("${rabbitmq.routing-key.insufficient-balance}")
    private String insufficientBalanceKey;

    public void publishTripCompleted(UUID tripId, UUID userId, UUID passId,
                                     BigDecimal amount, BigDecimal balanceAfter,
                                     TransportType transportType) {
        TripCompletedEvent event = TripCompletedEvent.builder()
                .tripId(tripId)
                .userId(userId)
                .passId(passId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .transportType(transportType)
                .completedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, tripCompletedKey, event);
        log.info("[TripEventPublisher] ✅ TRIP_COMPLETED publié - tripId={}", tripId);
    }

    public void publishPricingFallback(UUID tripId, UUID passId, String reason,
                                       BigDecimal fallbackAmount, TransportType transportType) {
        PricingFallbackEvent event = PricingFallbackEvent.builder()
                .tripId(tripId)
                .passId(passId)
                .reason(reason)
                .usedFallbackAmount(fallbackAmount)
                .transportType(transportType)
                .occurredAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, pricingFallbackKey, event);
        log.warn("[TripEventPublisher] ⚡ PRICING_FALLBACK publié - tripId={}", tripId);
    }

    // ✅ NOUVEAU : notification solde insuffisant
    public void publishInsufficientBalance(UUID userId, UUID passId, BigDecimal currentBalance) {
        InsufficientBalanceEvent event = InsufficientBalanceEvent.builder()
                .userId(userId)
                .passId(passId)
                .currentBalance(currentBalance)
                .occurredAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, insufficientBalanceKey, event);
        log.warn("[TripEventPublisher] ⚠️ INSUFFICIENT_BALANCE publié - userId={}, solde={} FCFA",
                userId, currentBalance);
    }
}
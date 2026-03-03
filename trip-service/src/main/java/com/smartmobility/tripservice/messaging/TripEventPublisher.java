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

    @Value("${rabbitmq.exchange.name}")                    private String exchange;
    @Value("${rabbitmq.routing-key.trip-started}")         private String tripStartedKey;
    @Value("${rabbitmq.routing-key.trip-completed}")       private String tripCompletedKey;
    @Value("${rabbitmq.routing-key.pricing-fallback}")     private String pricingFallbackKey;
    @Value("${rabbitmq.routing-key.insufficient-balance}") private String insufficientBalanceKey;
    @Value("${rabbitmq.routing-key.low-balance}")          private String lowBalanceKey;
    @Value("${rabbitmq.routing-key.daily-limit}")          private String dailyLimitKey;

    public void publishTripStarted(UUID tripId, UUID userId, UUID passId,
                                   String origin, String destination,
                                   BigDecimal estimatedFare, TransportType transportType) {
        var event = TripStartedEvent.builder()
                .tripId(tripId).userId(userId).passId(passId)
                .origin(origin).destination(destination)
                .estimatedFare(estimatedFare).transportType(transportType.name())
                .occurredAt(LocalDateTime.now()).build();
        rabbitTemplate.convertAndSend(exchange, tripStartedKey, event);
        log.info("[TripEventPublisher] 🚌 TRIP_STARTED publié - tripId={}", tripId);
    }

    public void publishTripCompleted(UUID tripId, UUID userId, UUID passId,
                                     BigDecimal amount, BigDecimal balanceAfter,
                                     TransportType transportType) {
        var event = TripCompletedEvent.builder()
                .tripId(tripId).userId(userId).passId(passId)
                .amount(amount).balanceAfter(balanceAfter)
                .transportType(TransportType.valueOf(transportType.name())).completedAt(LocalDateTime.now()).build();
        rabbitTemplate.convertAndSend(exchange, tripCompletedKey, event);
        log.info("[TripEventPublisher] ✅ TRIP_COMPLETED publié - tripId={}, montant={}", tripId, amount);
    }

    public void publishLowBalance(UUID userId, UUID passId, BigDecimal balance) {
        var event = LowBalanceEvent.builder()
                .userId(userId).passId(passId)
                .currentBalance(balance).occurredAt(LocalDateTime.now()).build();
        rabbitTemplate.convertAndSend(exchange, lowBalanceKey, event);
        log.warn("[TripEventPublisher] ⚠️ LOW_BALANCE publié - userId={}, solde={}", userId, balance);
    }

    public void publishInsufficientBalance(UUID userId, UUID passId, BigDecimal currentBalance) {
        var event = InsufficientBalanceEvent.builder()
                .userId(userId).passId(passId)
                .currentBalance(currentBalance).occurredAt(LocalDateTime.now()).build();
        rabbitTemplate.convertAndSend(exchange, insufficientBalanceKey, event);
        log.warn("[TripEventPublisher] ⚠️ INSUFFICIENT_BALANCE publié - userId={}", userId);
    }

    public void publishPricingFallback(UUID tripId, UUID passId,
                                       String reason, BigDecimal usedFallbackAmount,
                                       TransportType transportType) {
        var event = PricingFallbackEvent.builder()
                .tripId(tripId)
                .passId(passId)
                .transportType(transportType)
                .usedFallbackAmount(usedFallbackAmount)
                .reason(reason != null ? reason : "Pricing Service indisponible — tarif standard appliqué")
                .occurredAt(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(exchange, pricingFallbackKey, event);
        log.warn("[TripEventPublisher] ⚠️ PRICING_FALLBACK publié - tripId={}, montant={} FCFA",
                tripId, usedFallbackAmount);
    }

    public void publishDailyLimitReached(UUID userId, UUID passId,
                                         BigDecimal dailyLimit, BigDecimal totalSpentToday) {
        var event = DailyLimitReachedEvent.builder()
                .userId(userId).passId(passId)
                .dailyLimit(dailyLimit).totalSpentToday(totalSpentToday)
                .occurredAt(LocalDateTime.now()).build();
        rabbitTemplate.convertAndSend(exchange, dailyLimitKey, event);
        log.warn("[TripEventPublisher] 🚫 DAILY_LIMIT_REACHED publié - userId={}, total={} FCFA",
                userId, totalSpentToday);
    }
}
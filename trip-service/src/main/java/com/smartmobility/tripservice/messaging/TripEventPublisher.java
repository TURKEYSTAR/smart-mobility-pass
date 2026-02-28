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
    private String exchangeName;

    @Value("${rabbitmq.routing-key.trip-completed}")
    private String tripCompletedRoutingKey;

    @Value("${rabbitmq.routing-key.pricing-fallback}")
    private String pricingFallbackRoutingKey;

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

        log.info("[RabbitMQ] Publish TRIP_COMPLETED → tripId={}, montant={} FCFA, solde après={} FCFA",
                tripId, amount, balanceAfter);

        rabbitTemplate.convertAndSend(exchangeName, tripCompletedRoutingKey, event);
    }

    public void publishPricingFallbackUsed(UUID tripId, UUID passId,
                                           BigDecimal fallbackAmount,
                                           TransportType transportType) {

        PricingFallbackEvent event = PricingFallbackEvent.builder()
                .tripId(tripId)
                .passId(passId)
                .reason("PricingServiceDown")
                .usedFallbackAmount(fallbackAmount)
                .transportType(transportType)
                .occurredAt(LocalDateTime.now())
                .build();

        log.warn("[RabbitMQ] Publish PRICING_FALLBACK_USED → tripId={}, fallback={} FCFA, type={}",
                tripId, fallbackAmount, transportType);

        rabbitTemplate.convertAndSend(exchangeName, pricingFallbackRoutingKey, event);
    }
}

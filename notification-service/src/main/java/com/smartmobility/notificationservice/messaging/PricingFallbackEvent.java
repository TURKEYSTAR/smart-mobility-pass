package com.smartmobility.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Même structure que PricingFallbackEvent du Trip Service
 * Reçu via RabbitMQ queue: pricing.fallback.queue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingFallbackEvent {
    private UUID tripId;
    private UUID passId;
    private String reason;
    private BigDecimal usedFallbackAmount;
    private String transportType;
    private LocalDateTime occurredAt;
}
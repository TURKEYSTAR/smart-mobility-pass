package com.smartmobility.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Même structure que TripCompletedEvent du Trip Service
 * Reçu via RabbitMQ queue: trip.completed.queue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripCompletedEvent {
    private UUID tripId;
    private UUID userId;
    private UUID passId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String transportType;
    private LocalDateTime completedAt;
}
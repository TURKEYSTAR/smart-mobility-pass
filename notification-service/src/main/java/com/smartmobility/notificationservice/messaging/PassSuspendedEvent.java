package com.smartmobility.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Miroir de PassSuspendedEvent du user-service
 * Reçu via RabbitMQ queue: pass.suspended.queue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassSuspendedEvent {
    private UUID userId;
    private UUID passId;
    private String passNumber;
    private BigDecimal currentBalance;
    private LocalDateTime suspendedAt;
}

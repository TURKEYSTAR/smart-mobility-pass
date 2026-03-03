package com.smartmobility.tripservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStartedEvent {
    private UUID tripId;
    private UUID userId;
    private UUID passId;
    private String origin;
    private String destination;
    private BigDecimal estimatedFare;
    private String transportType;
    private LocalDateTime occurredAt;
}

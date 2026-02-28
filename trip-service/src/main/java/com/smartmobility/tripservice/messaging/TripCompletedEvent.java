package com.smartmobility.tripservice.messaging;

import com.smartmobility.tripservice.entity.TransportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
    private TransportType transportType;
    private LocalDateTime completedAt;
}

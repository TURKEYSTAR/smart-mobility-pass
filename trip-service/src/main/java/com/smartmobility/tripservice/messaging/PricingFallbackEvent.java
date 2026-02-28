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
public class PricingFallbackEvent {

    private UUID tripId;
    private UUID passId;
    private String reason;
    private BigDecimal usedFallbackAmount;
    private TransportType transportType;
    private LocalDateTime occurredAt;
}

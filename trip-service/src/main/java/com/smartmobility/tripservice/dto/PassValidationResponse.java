package com.smartmobility.tripservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassValidationResponse {

    private UUID userId;
    private UUID passId;
    private BigDecimal balance;
    private String tier;       // STANDARD, SILVER, GOLD, PLATINUM
    private String status;     // ACTIVE, SUSPENDED, EXPIRED, BLOCKED
    private boolean eligible;
    private String reason;
    private int totalTrips;
}

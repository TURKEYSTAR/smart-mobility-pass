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
public class BillingRequest {

    private UUID passId;
    private UUID tripId;
    private BigDecimal amount;
    private String description;
}

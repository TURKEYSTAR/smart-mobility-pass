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

    private UUID userId;
    private UUID tripId;
    private BigDecimal montant;
    private String description;
}
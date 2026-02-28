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
public class BillingResponse {

    private UUID transactionId;
    private BigDecimal balanceAfter;
    private String status;
}

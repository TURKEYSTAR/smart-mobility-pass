package com.smartmobility.tripservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareResultDTO {

    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private List<String> appliedDiscounts;
    private boolean cappedByDailyLimit;
    private boolean fallbackUsed;
    private String note;
}

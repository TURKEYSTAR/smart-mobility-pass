package com.smartmobility.tripservice.dto;

import com.smartmobility.tripservice.entity.TransportType;
import com.smartmobility.tripservice.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponse {

    private UUID tripId;
    private UUID userId;
    private UUID passId;
    private TransportType transportType;
    private String origin;
    private String destination;
    private Double distanceKm;
    private TripStatus status;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal computedFare;
    private BigDecimal balanceAfter;
    private UUID transactionId;
    private List<String> appliedDiscounts;
    private boolean fallbackUsed;
    private LocalDateTime createdAt;
    private String message;
}

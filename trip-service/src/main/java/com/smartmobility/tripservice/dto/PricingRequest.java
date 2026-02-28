package com.smartmobility.tripservice.dto;

import com.smartmobility.tripservice.entity.TransportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRequest {

    private UUID tripId;
    private TransportType transportType;
    private Double distanceKm;
    private LocalDateTime departureTime;
    private UUID passId;
    private String passTier;
    private int totalTrips;
}

package com.smartmobility.pricingservice.dto;

import com.smartmobility.pricingservice.entity.TransportType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Requête de calcul de tarif (reçue du Trip Service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRequest {

    @NotNull(message = "tripId est requis")
    private UUID tripId;

    @NotNull(message = "Le type de transport est requis")
    private TransportType transportType;

    @NotNull(message = "La distance est requise")
    @Positive(message = "La distance doit être positive")
    private Double distanceKm;

    private LocalDateTime departureTime;

    @NotNull(message = "passId est requis")
    private UUID passId;

    private String passTier;  // STANDARD, SILVER, GOLD, PLATINUM
    private int totalTrips;   // Nb total de trajets pour fidélité
}

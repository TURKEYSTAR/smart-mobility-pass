package com.smartmobility.pricingservice.dto;

import com.smartmobility.pricingservice.entity.TransportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Requête de calcul de tarif (reçue du Trip Service).
 * ⚠️ distanceKm supprimé — tarification par zones désormais.
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

    @NotBlank(message = "La ligne est requise")
    private String ligneId;          // ex: "BRT_B1", "TER_L1", "BUS_L1"

    @NotBlank(message = "L'arrêt de départ est requis")
    private String arretDepartId;    // ex: "BRT_PETERSEN"

    @NotBlank(message = "L'arrêt d'arrivée est requis")
    private String arretArriveeId;   // ex: "BRT_PARCELLES"

    private LocalDateTime departureTime;

    @NotNull(message = "passId est requis")
    private UUID passId;

    private String passTier;   // STANDARD, SILVER, GOLD, PLATINUM
    private int totalTrips;    // Nb total de trajets pour fidélité
}
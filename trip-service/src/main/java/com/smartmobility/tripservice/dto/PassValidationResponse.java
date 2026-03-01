package com.smartmobility.tripservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PassValidationResponse — mappé sur PassResponse du user-service.
 * <p>
 * user-service retourne :
 * { id, passNumber, status (ACTIVE/SUSPENDU/EXPIRE), solde, userId, ... }
 * <p>
 * On mappe :
 * solde    → balance
 * status   → status (String)
 * userId   → userId
 * id       → passId
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassValidationResponse {

    private UUID userId;
    private UUID id;          // passId dans user-service
    private BigDecimal solde; // solde dans user-service
    private String status;    // ACTIVE, SUSPENDU, EXPIRE
    private String passNumber;

    // Champs calculés/dérivés (non retournés par user-service, valeurs par défaut)
    private String tier = "STANDARD";
    private int totalTrips = 0;

    // Aliases pour compatibilité avec TripService
    public UUID getPassId() {
        return id;
    }

    public BigDecimal getBalance() {
        return solde;
    }

    public boolean isEligible() {
        return "ACTIVE".equals(status);
    }
}
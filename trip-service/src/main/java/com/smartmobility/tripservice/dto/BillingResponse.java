package com.smartmobility.tripservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BillingResponse — mappé sur TransactionResponse du billing-service.
 * <p>
 * billing-service retourne :
 * { id, userId, tripId, montant, type, status, description, createdAt, soldeApresOperation }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingResponse {

    private UUID id;                      // id de la transaction
    private BigDecimal soldeApresOperation; // solde après débit
    private String status;                // SUCCESS / FAILED

    // Aliases pour compatibilité avec TripService
    public UUID getTransactionId() {
        return id;
    }

    public BigDecimal getBalanceAfter() {
        return soldeApresOperation;
    }
}
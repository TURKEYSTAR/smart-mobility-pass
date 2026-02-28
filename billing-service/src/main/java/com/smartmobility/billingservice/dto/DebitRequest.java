package com.smartmobility.billingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DebitRequest {

    @NotNull(message = "L'identifiant utilisateur est obligatoire")
    private Long userId;

    // tripId peut être null pour un débit manuel (hors trajet)
    private Long tripId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private BigDecimal montant;

    private String description;

    public DebitRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
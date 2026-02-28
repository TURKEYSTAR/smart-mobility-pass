package com.smartmobility.billingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class RechargeRequest {

    @NotNull(message = "L'identifiant utilisateur est obligatoire")
    private UUID userId;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "100.0", message = "Le montant minimum de recharge est de 100 FCFA")
    private BigDecimal montant;

    public RechargeRequest() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
}
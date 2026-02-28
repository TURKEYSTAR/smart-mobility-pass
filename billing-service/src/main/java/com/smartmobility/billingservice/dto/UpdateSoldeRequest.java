package com.smartmobility.billingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateSoldeRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private BigDecimal montant;

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public UpdateSoldeRequest() {
    }

    public UpdateSoldeRequest(BigDecimal montant) {
        this.montant = montant;
    }

    public @NotNull(message = "Le montant est obligatoire") @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif") BigDecimal getMontant() {
        return montant;
    }

}
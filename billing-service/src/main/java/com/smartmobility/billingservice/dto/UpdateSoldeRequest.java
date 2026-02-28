package com.smartmobility.billingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class UpdateSoldeRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
    private Double montant;

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public UpdateSoldeRequest() {
    }

    public @NotNull(message = "Le montant est obligatoire") @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif") Double getMontant() {
        return montant;
    }

}
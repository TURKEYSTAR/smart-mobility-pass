package com.smartmobility.userservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateSoldeRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal montant;

    public UpdateSoldeRequest() {}

    public UpdateSoldeRequest(BigDecimal montant) {
        this.montant = montant;
    }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}
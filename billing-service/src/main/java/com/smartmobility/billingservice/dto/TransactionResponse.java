package com.smartmobility.billingservice.dto;

import com.smartmobility.billingservice.entity.TransactionStatus;
import com.smartmobility.billingservice.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private Long userId;
    private Long tripId;
    private BigDecimal montant;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;

    // Solde restant après l'opération (récupéré depuis user-service)
    private BigDecimal soldeApresOperation;

    public TransactionResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getSoldeApresOperation() {
        return soldeApresOperation;
    }

    public void setSoldeApresOperation(BigDecimal soldeApresOperation) {
        this.soldeApresOperation = soldeApresOperation;
    }
}
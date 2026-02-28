package com.smartmobility.userservice.dto;

import com.smartmobility.userservice.entity.PassStatus;

import java.time.LocalDateTime;

public class PassResponse {

    private Long id;
    private String passNumber;
    private PassStatus status;
    private Double solde;
    private LocalDateTime createdAt;
    private LocalDateTime expirationDate;

    // userId utile pour les appels inter-services
    private Long userId;

    // Champ calculé : indique si le pass est expiré au moment de la réponse
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    public PassResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassNumber() {
        return passNumber;
    }

    public void setPassNumber(String passNumber) {
        this.passNumber = passNumber;
    }

    public PassStatus getStatus() {
        return status;
    }

    public void setStatus(PassStatus status) {
        this.status = status;
    }

    public Double getSolde() {
        return solde;
    }

    public void setSolde(Double solde) {
        this.solde = solde;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
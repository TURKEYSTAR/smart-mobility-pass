package com.smartmobility.userservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class MobilityPass {
    @Id
    private Long id;

    String passNumber; // généré (UUID ou format custom)
    PassStatus status; // ACTIVE, SUSPENDED
    Double solde;
    LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @JoinColumn(name = "user_id")
    @OneToOne
    User user;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}

package com.smartmobility.userservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mobility_pass")
public class MobilityPass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String passNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassStatus status;

    // BigDecimal pour la précision financière (FCFA)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal solde;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    // Relation inverse — le User possède le côté propriétaire (@JoinColumn)
    @OneToOne(mappedBy = "mobilityPass")
    private User user;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public BigDecimal getSolde() {
        return solde;
    }

    public void setSolde(BigDecimal solde) {
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
}
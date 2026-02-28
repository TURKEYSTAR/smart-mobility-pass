package com.smartmobility.billingservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // userId — Long car c'est l'ID de l'entité User en BDD
    @Column(nullable = false)
    private UUID userId;

    // passId — UUID comme dans le trip-service
    @Column(nullable = false)
    private UUID passId;

    // tripId — UUID, null si c'est une recharge
    private UUID tripId;

    // BigDecimal pour la précision financière (comme trip-service)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TypeTransport typeTransport;

    private String description;

    // Solde après la transaction (BigDecimal)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal soldeApres;

    // Statut : SUCCESS, FAILED
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getPassId() { return passId; }
    public void setPassId(UUID passId) { this.passId = passId; }

    public UUID getTripId() { return tripId; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TypeTransport getTypeTransport() { return typeTransport; }
    public void setTypeTransport(TypeTransport typeTransport) { this.typeTransport = typeTransport; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getSoldeApres() { return soldeApres; }
    public void setSoldeApres(BigDecimal soldeApres) { this.soldeApres = soldeApres; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
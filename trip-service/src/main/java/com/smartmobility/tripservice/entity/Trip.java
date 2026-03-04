package com.smartmobility.tripservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pass_id", nullable = false)
    private UUID passId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    private TransportType transportType;

    // ── Nouvelle structure zone/ligne ──────────────────────────────

    @Column(name = "ligne_id", nullable = false, length = 20)
    private String ligneId;          // ex: "BRT_B1"

    @Column(name = "ligne_nom", length = 100)
    private String ligneNom;         // ex: "B1 — Omnibus"

    @Column(name = "arret_depart_id", nullable = false, length = 30)
    private String arretDepartId;    // ex: "BRT_PETERSEN"

    @Column(name = "arret_depart_nom", length = 100)
    private String arretDepartNom;   // ex: "Papa Gueye Fall (Petersen)"

    @Column(name = "arret_arrivee_id", nullable = false, length = 30)
    private String arretArriveeId;

    @Column(name = "arret_arrivee_nom", length = 100)
    private String arretArriveeNom;

    @Column(name = "zone_depart")
    private Integer zoneDepart;      // 1, 2 ou 3

    @Column(name = "zone_arrivee")
    private Integer zoneArrivee;

    // ── Temps ──────────────────────────────────────────────────────

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    // ── Statut & tarif ────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.IN_PROGRESS;

    @Column(name = "computed_fare", precision = 10, scale = 2)
    private BigDecimal computedFare;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Retourne un label lisible pour les notifications.
     * ex: "BRT B1 — Petersen → Parcelles"
     */
    public String getLabel() {
        String depart  = arretDepartNom  != null ? arretDepartNom  : arretDepartId;
        String arrivee = arretArriveeNom != null ? arretArriveeNom : arretArriveeId;
        return transportType.name() + " — " + depart + " → " + arrivee;
    }

    public boolean isValid() {
        return userId != null
                && passId != null
                && transportType != null
                && ligneId != null && !ligneId.isBlank()
                && arretDepartId != null && !arretDepartId.isBlank()
                && arretArriveeId != null && !arretArriveeId.isBlank();
    }
}
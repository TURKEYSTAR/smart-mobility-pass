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

    @Column(name = "origin", nullable = false, length = 255)
    private String origin;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.INITIATED;

    @Column(name = "computed_fare", precision = 10, scale = 2)
    private BigDecimal computedFare;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public long calculateDuration() {
        if (departureTime != null && arrivalTime != null) {
            return java.time.Duration.between(departureTime, arrivalTime).toMinutes();
        }
        return 0;
    }

    public boolean isValid() {
        return userId != null
                && passId != null
                && transportType != null
                && origin != null && !origin.isBlank()
                && destination != null && !destination.isBlank()
                && distanceKm != null && distanceKm > 0;
    }
}
package com.smartmobility.pricingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historique des calculs tarifaires
 * Table: fare_calculations (pricing_db)
 */
@Entity
@Table(name = "fare_calculations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FareCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false, unique = true)
    private UUID tripId;

    @Column(name = "pass_id", nullable = false)
    private UUID passId;

    @Column(name = "base_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "discount_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 8, scale = 2)
    private BigDecimal finalAmount;

    // JSON des réductions appliquées (ex: ["OFF_PEAK -20%", "LOYALTY -5%"])
    @Column(name = "applied_discounts", columnDefinition = "TEXT")
    private String appliedDiscounts;

    @Column(name = "capped_by_daily_limit")
    @Builder.Default
    private boolean cappedByDailyLimit = false;

    @Column(name = "fallback_used")
    @Builder.Default
    private boolean fallbackUsed = false;

    @CreationTimestamp
    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt;
}

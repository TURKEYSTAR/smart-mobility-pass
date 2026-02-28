package com.smartmobility.pricingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Politiques de réduction (heures creuses, fidélité, tier...)
 * Table: discount_policies (pricing_db)
 */
@Entity
@Table(name = "discount_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DiscountPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DiscountType type;

    @Column(name = "discount_value", precision = 5, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_trips")
    @Builder.Default
    private int minTrips = 0;

    @Column(name = "min_spent", precision = 8, scale = 2)
    private BigDecimal minSpent;

    @Column(name = "daily_cap", precision = 8, scale = 2)
    private BigDecimal dailyCap;

    // Tier requis pour bénéficier de la réduction (null = tous les tiers)
    @Column(name = "application_tier", length = 20)
    private String applicationTier;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Vérifie si cette politique s'applique au pass donné
     */
    public boolean isApplicable(String passTier, int totalTrips) {
        if (!active) return false;
        if (applicationTier != null && !applicationTier.equals(passTier)) return false;
        if (minTrips > 0 && totalTrips < minTrips) return false;
        return true;
    }
}

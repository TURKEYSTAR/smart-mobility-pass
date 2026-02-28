package com.smartmobility.pricingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Règles tarifaires par type de transport
 * Table: pricing_rules (pricing_db)
 */
@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, unique = true)
    private TransportType transportType;

    @Column(name = "base_price", nullable = false, precision = 8, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "price_per_km", nullable = false, precision = 5, scale = 2)
    private BigDecimal pricePerKm;

    @Column(name = "off_peak_discount", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal offPeakDiscount = BigDecimal.valueOf(20.0);

    @Column(name = "daily_cap", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal dailyCap = BigDecimal.valueOf(2000);

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

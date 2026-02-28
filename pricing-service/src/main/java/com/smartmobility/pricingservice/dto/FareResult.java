package com.smartmobility.pricingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat du calcul tarifaire (retourné au Trip Service)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareResult {
    private BigDecimal baseAmount;          // Prix de base calculé
    private BigDecimal discountAmount;      // Total des réductions appliquées
    private BigDecimal finalAmount;         // Montant final à débiter
    private List<String> appliedDiscounts;  // Liste des réductions appliquées
    private boolean cappedByDailyLimit;     // True si plafond journalier atteint
    private boolean fallbackUsed;           // False (toujours false côté Pricing Service)
    private String note;                    // Info supplémentaire
}

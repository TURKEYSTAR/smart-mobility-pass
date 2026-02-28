package com.smartmobility.pricingservice.entity;

/**
 * Types de réduction disponibles
 */
public enum DiscountType {
    PERCENTAGE,    // Pourcentage sur le montant total
    FIXED_AMOUNT,  // Montant fixe déduit
    OFF_PEAK,      // Réduction heures creuses
    LOYALTY,       // Réduction fidélité (basée sur nb de trajets)
    DAILY_CAP      // Plafonnement journalier
}

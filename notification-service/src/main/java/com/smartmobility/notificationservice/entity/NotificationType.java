package com.smartmobility.notificationservice.entity;

public enum NotificationType {
    // ── Trajets ───────────────────────────────────────────────────────────────
    TRIP_STARTED,           // Trajet initié, débit effectué
    TRIP_COMPLETED,         // Trajet terminé par l'utilisateur
    PRICING_FALLBACK,       // Tarif standard appliqué (pricing-service down)

    // ── Solde ─────────────────────────────────────────────────────────────────
    LOW_BALANCE,            // Solde faible après trajet
    INSUFFICIENT_BALANCE,   // Solde insuffisant, trajet refusé
    DAILY_LIMIT_REACHED,    // Plafond journalier atteint
    RECHARGE_REFUSED,       // Recharge refusée (pass suspendu)

    // ── Pass ──────────────────────────────────────────────────────────────────
    PASS_SUSPENDED,         // Pass suspendu par admin
    PASS_ACTIVATED,         // Pass réactivé par admin
}
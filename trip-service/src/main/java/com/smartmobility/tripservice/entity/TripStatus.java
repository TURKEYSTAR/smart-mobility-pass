package com.smartmobility.tripservice.entity;

public enum TripStatus {
    IN_PROGRESS,  // Trajet démarré — débit effectué, en cours
    COMPLETED,    // Trajet terminé par l'utilisateur
    CANCELLED     // Trajet annulé — pas de remboursement
}
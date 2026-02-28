package com.smartmobility.billingservice.entity;

public enum TransactionStatus {
    SUCCESS,  // Débit ou recharge effectué avec succès
    FAILED    // Echec : solde insuffisant, pass suspendu/expiré
}
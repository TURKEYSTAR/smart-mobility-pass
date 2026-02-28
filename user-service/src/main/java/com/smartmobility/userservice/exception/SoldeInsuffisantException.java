package com.smartmobility.userservice.exception;


public class SoldeInsuffisantException extends RuntimeException {

    public SoldeInsuffisantException(Double soldeActuel, Double montantDemande) {
        super("Solde insuffisant. Solde actuel : " + soldeActuel + " FCFA, montant demandé : " + montantDemande + " FCFA");
    }
}
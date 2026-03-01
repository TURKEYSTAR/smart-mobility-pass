package com.smartmobility.userservice.exception;

import java.math.BigDecimal;

public class SoldeInsuffisantException extends RuntimeException {

    public SoldeInsuffisantException(BigDecimal soldeActuel, BigDecimal montantDemande) {
        super("Solde insuffisant. Solde actuel : " + soldeActuel + " FCFA, montant demandé : " + montantDemande + " FCFA");
    }
}
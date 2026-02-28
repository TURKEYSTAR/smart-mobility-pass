package com.smartmobility.billingservice.exception;

/**
 * Exception levée quand une transaction n'est pas trouvée en BDD.
 * ID en Long — cohérent avec Transaction.id et BillingService.
 */
public class TransactionNotFoundException extends RuntimeException {

    private final Long transactionId;

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction non trouvée : " + transactionId);
        this.transactionId = transactionId;
    }

    public Long getTransactionId() {
        return transactionId;
    }
}
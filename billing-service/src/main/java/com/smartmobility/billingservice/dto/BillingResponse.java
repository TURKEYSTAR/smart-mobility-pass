package com.smartmobility.billingservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BillingResponse — renvoyé au trip-service après un débit.
 *
 * Correspond exactement au BillingResponse du trip-service :
 *   private UUID transactionId;
 *   private BigDecimal balanceAfter;
 *   private String status;
 */
public class BillingResponse {

    private UUID transactionId;      // UUID de la transaction créée
    private BigDecimal balanceAfter; // solde après débit
    private String status;           // "SUCCESS" ou "FAILED"

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
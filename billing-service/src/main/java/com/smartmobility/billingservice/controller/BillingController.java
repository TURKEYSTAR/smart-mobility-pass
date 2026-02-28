package com.smartmobility.billingservice.controller;

import com.smartmobility.billingservice.dto.DebitRequest;
import com.smartmobility.billingservice.dto.RechargeRequest;
import com.smartmobility.billingservice.dto.TransactionResponse;
import com.smartmobility.billingservice.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // ── POST /api/billing/debit ───────────────────────────────────────────────
    // ⚠️  Appelé UNIQUEMENT par trip-service via Feign automatiquement.
    //     Aucun utilisateur ne doit appeler ce endpoint directement.
    //     Le trip-service s'identifie avec le header X-Internal-Service.
    @PostMapping("/debit")
    public ResponseEntity<?> debiter(
            @RequestBody DebitRequest request,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalService) {

        if (!"trip-service".equals(internalService)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Endpoint réservé aux services internes"));
        }

        try {
            TransactionResponse response = billingService.debiter(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/billing/recharge ────────────────────────────────────────────
    // USER recharge son propre pass — MANAGER/ADMIN peuvent tout recharger
    @PostMapping("/recharge")
    public ResponseEntity<?> recharger(
            @RequestBody RechargeRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean estProprietaire = String.valueOf(request.getUserId()).equals(userId);
        boolean estPrivilegie   = hasRole(userRole, "MANAGER", "ADMIN");

        if (!estProprietaire && !estPrivilegie) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès refusé : vous ne pouvez recharger que votre propre pass"));
        }

        try {
            TransactionResponse response = billingService.recharger(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/billing/history/{userId} ─────────────────────────────────────
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> obtenirHistorique(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean estProprietaire = String.valueOf(userId).equals(currentUserId);
        boolean estPrivilegie   = hasRole(userRole, "MANAGER", "ADMIN");

        if (!estProprietaire && !estPrivilegie) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }

        return ResponseEntity.ok(billingService.obtenirHistorique(userId));
    }

    // ── GET /api/billing/transaction/{id} ─────────────────────────────────────
    @GetMapping("/transaction/{id}")
    public ResponseEntity<?> obtenirTransaction(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        try {
            TransactionResponse transaction = billingService.obtenirTransaction(id);

            boolean estProprietaire = String.valueOf(transaction.getUserId()).equals(userId);
            boolean estPrivilegie   = hasRole(userRole, "MANAGER", "ADMIN");

            if (!estProprietaire && !estPrivilegie) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }

            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean hasRole(String userRole, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(userRole)) return true;
        }
        return false;
    }
}
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

/**
 * BillingController
 *
 * Tous les endpoints sont protégés par la Gateway (JwtAuthFilter).
 * L'identité de l'appelant vient des headers X-User-Id / X-User-Role.
 *
 * POST /api/billing/debit              → débiter le pass (MANAGER / ADMIN)
 * POST /api/billing/recharge           → recharger le pass (USER / MANAGER / ADMIN)
 * GET  /api/billing/history/{userId}   → historique (propriétaire ou MANAGER/ADMIN)
 * GET  /api/billing/transaction/{id}   → détail transaction (propriétaire ou MANAGER/ADMIN)
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    // ── POST /api/billing/debit ───────────────────────────────────────────────
    // Paiement d'un trajet — réservé MANAGER / ADMIN
    @PostMapping("/debit")
    public ResponseEntity<?> debiter(
            @RequestBody DebitRequest request,
            @RequestHeader("X-User-Role") String userRole) {

        if (!hasRole(userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès refusé : réservé aux MANAGER et ADMIN"));
        }

        try {
            TransactionResponse response = billingService.debiter(request, userRole);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/billing/recharge ────────────────────────────────────────────
    // Recharge du pass — USER peut recharger son propre pass
    @PostMapping("/recharge")
    public ResponseEntity<?> recharger(
            @RequestBody RechargeRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        // Un USER ne peut recharger que son propre pass
        boolean estProprietaire = String.valueOf(request.getUserId()).equals(userId);
        boolean estPrivilegie   = hasRole(userRole, "MANAGER", "ADMIN");

        if (!estProprietaire && !estPrivilegie) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès refusé : vous ne pouvez recharger que votre propre pass"));
        }

        try {
            TransactionResponse response = billingService.recharger(request, userRole);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/billing/history/{userId} ─────────────────────────────────────
    // Historique des transactions
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> obtenirHistorique(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean estProprietaire = String.valueOf(userId).equals(currentUserId);
        boolean estPrivilegie   = hasRole(userRole, "MANAGER", "ADMIN");

        if (!estProprietaire && !estPrivilegie) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès refusé"));
        }

        List<TransactionResponse> historique = billingService.obtenirHistorique(userId);
        return ResponseEntity.ok(historique);
    }

    // ── GET /api/billing/transaction/{id} ─────────────────────────────────────
    // Détail d'une transaction
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
            return ResponseEntity.status(404)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────
    private boolean hasRole(String userRole, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(userRole)) return true;
        }
        return false;
    }
}
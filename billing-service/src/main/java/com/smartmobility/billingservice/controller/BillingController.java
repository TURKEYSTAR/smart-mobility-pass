package com.smartmobility.billingservice.controller;

import com.smartmobility.billingservice.dto.DebitRequest;
import com.smartmobility.billingservice.dto.RechargeRequest;
import com.smartmobility.billingservice.dto.TransactionResponse;
import com.smartmobility.billingservice.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * POST /api/billing/debit
     * Réservé au trip-service (header X-Internal-Service requis).
     */
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

    /**
     * POST /api/billing/recharge
     * Accessible au propriétaire du pass OU à un ADMIN.
     */
    @PostMapping("/recharge")
    public ResponseEntity<?> recharger(
            @RequestBody RechargeRequest request,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean estProprietaire = request.getUserId().toString().equals(userId);
        boolean estAdmin        = "ADMIN".equalsIgnoreCase(userRole);

        if (!estProprietaire && !estAdmin) {
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

    /**
     * GET /api/billing/history/{userId}
     * Historique des transactions — propriétaire OU ADMIN.
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> obtenirHistorique(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        boolean estProprietaire = userId.toString().equals(currentUserId);
        boolean estAdmin        = "ADMIN".equalsIgnoreCase(userRole);

        if (!estProprietaire && !estAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        return ResponseEntity.ok(billingService.obtenirHistorique(userId));
    }

    /**
     * GET /api/billing/transaction/{id}
     * Détail d'une transaction — propriétaire OU ADMIN.
     */
    @GetMapping("/transaction/{id}")
    public ResponseEntity<?> obtenirTransaction(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        try {
            TransactionResponse transaction = billingService.obtenirTransaction(id);

            boolean estProprietaire = transaction.getUserId().toString().equals(userId);
            boolean estAdmin        = "ADMIN".equalsIgnoreCase(userRole);

            if (!estProprietaire && !estAdmin) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
            return ResponseEntity.ok(transaction);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/billing/stats
     * Stats du jour — ADMIN uniquement.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> obtenirStats(
            @RequestHeader("X-User-Role") String userRole) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
        return ResponseEntity.ok(billingService.obtenirStatsJour());
    }

    /**
     * GET /api/billing/daily-total/{passId}
     * Total des débits du jour pour un pass.
     * Appelé par le pricing-service pour vérifier le plafond journalier (2000 FCFA).
     * Endpoint interne — pas de vérification JWT (appelé service-à-service).
     */
    @GetMapping("/daily-total/{passId}")
    public ResponseEntity<BigDecimal> getDailyTotal(@PathVariable UUID passId) {
        return ResponseEntity.ok(billingService.getDailyTotal(passId));
    }
}
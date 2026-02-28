package com.smartmobility.userservice.controller;

import com.smartmobility.userservice.dto.PassResponse;
import com.smartmobility.userservice.dto.UpdateSoldeRequest;
import com.smartmobility.userservice.service.PassMobilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * PassMobilityController — gestion du Mobility Pass uniquement.
 *
 * Délègue à : PassMobilityService
 *
 * Endpoints :
 *   GET /api/users/{id}/pass              → consulter le pass et le solde
 *   PUT /api/users/{id}/pass/suspendre    → suspendre   (MANAGER / ADMIN)
 *   PUT /api/users/{id}/pass/activer      → réactiver   (MANAGER / ADMIN)
 *   PUT /api/users/{id}/pass/renouveler   → renouveler  (propriétaire ou MANAGER/ADMIN)
 *   PUT /api/users/{id}/pass/debiter      → débiter     (MANAGER / ADMIN / billing-service)
 *   PUT /api/users/{id}/pass/recharger    → recharger   (propriétaire ou MANAGER/ADMIN)
 *
 * Sécurité assurée par la Gateway → headers X-User-Id / X-User-Role injectés.
 * Pas de Spring Security ici.
 */
@RestController
@RequestMapping("/api/users/{id}/pass")
public class PassMobilityController {

    private final PassMobilityService passMobilityService;

    public PassMobilityController(PassMobilityService passMobilityService) {
        this.passMobilityService = passMobilityService;
    }

    // ── GET /api/users/{id}/pass ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> obtenirPass(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrRole(id, userId, userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        return ResponseEntity.ok(passMobilityService.obtenirPass(id));
    }

    // ── PUT /api/users/{id}/pass/suspendre ────────────────────────────────────
    @PutMapping("/suspendre")
    public ResponseEntity<?> suspendrePass(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!hasRole(userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403)
                    .body("Accès refusé : réservé aux MANAGER et ADMIN");
        }

        return ResponseEntity.ok(passMobilityService.suspendrePass(id));
    }

    // ── PUT /api/users/{id}/pass/activer ──────────────────────────────────────
    @PutMapping("/activer")
    public ResponseEntity<?> activerPass(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!hasRole(userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403)
                    .body("Accès refusé : réservé aux MANAGER et ADMIN");
        }

        return ResponseEntity.ok(passMobilityService.activerPass(id));
    }

    // ── PUT /api/users/{id}/pass/renouveler ───────────────────────────────────
    @PutMapping("/renouveler")
    public ResponseEntity<?> renouvellerPass(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrRole(id, userId, userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        return ResponseEntity.ok(passMobilityService.renouvellerPass(id));
    }

    // ── PUT /api/users/{id}/pass/debiter ──────────────────────────────────────
    // Appelé par billing-service via Feign (interne)
    @PutMapping("/debiter")
    public ResponseEntity<?> debiterSolde(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateSoldeRequest request) {

        if (!hasRole(userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        return ResponseEntity.ok(passMobilityService.debiterSolde(id, request));
    }

    // ── PUT /api/users/{id}/pass/recharger ────────────────────────────────────
    @PutMapping("/recharger")
    public ResponseEntity<?> rechargerSolde(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateSoldeRequest request) {

        if (!isOwnerOrRole(id, userId, userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403).body("Accès refusé");
        }

        return ResponseEntity.ok(passMobilityService.rechargerSolde(id, request));
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private boolean isOwnerOrRole(Long resourceId, String userId,
                                  String userRole, String... allowedRoles) {
        if (String.valueOf(resourceId).equals(userId)) return true;
        return hasRole(userRole, allowedRoles);
    }

    private boolean hasRole(String userRole, String... allowedRoles) {
        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(userRole)) return true;
        }
        return false;
    }
}
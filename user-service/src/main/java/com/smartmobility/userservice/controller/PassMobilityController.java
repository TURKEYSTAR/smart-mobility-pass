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

    // GET /api/users/{id}/pass — son propre pass OU ADMIN
    @GetMapping
    public ResponseEntity<?> obtenirPass(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrAdmin(id, userId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.obtenirPass(id));
    }

    // PUT /pass/suspendre — ADMIN uniquement
    @PutMapping("/suspendre")
    public ResponseEntity<?> suspendrePass(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        return ResponseEntity.ok(passMobilityService.suspendrePass(id));
    }

    // PUT /pass/activer — ADMIN uniquement
    @PutMapping("/activer")
    public ResponseEntity<?> activerPass(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        return ResponseEntity.ok(passMobilityService.activerPass(id));
    }

    // PUT /pass/renouveler — son propre pass OU ADMIN
    @PutMapping("/renouveler")
    public ResponseEntity<?> renouvellerPass(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrAdmin(id, userId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.renouvellerPass(id));
    }

    // PUT /pass/debiter — appelé UNIQUEMENT par billing-service (interne)
    // billing-service envoie X-User-Role: ADMIN
    @PutMapping("/debiter")
    public ResponseEntity<?> debiterSolde(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateSoldeRequest request) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.debiterSolde(id, request));
    }

    // PUT /pass/recharger — son propre pass OU ADMIN
    @PutMapping("/recharger")
    public ResponseEntity<?> rechargerSolde(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateSoldeRequest request) {

        if (!isOwnerOrAdmin(id, userId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.rechargerSolde(id, request));
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────
    private boolean isOwnerOrAdmin(Long resourceId, String userId, String userRole) {
        return String.valueOf(resourceId).equals(userId) || isAdmin(userRole);
    }

    private boolean isAdmin(String userRole) {
        return "ADMIN".equalsIgnoreCase(userRole);
    }
}
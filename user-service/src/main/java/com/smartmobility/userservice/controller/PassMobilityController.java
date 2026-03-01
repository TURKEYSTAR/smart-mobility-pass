package com.smartmobility.userservice.controller;

import com.smartmobility.userservice.dto.PassResponse;
import com.smartmobility.userservice.dto.UpdateSoldeRequest;
import com.smartmobility.userservice.service.PassMobilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * PassMobilityController — gestion du Mobility Pass.
 *
 * Rôles :
 *   USER  → consulter son pass, recharger son pass, renouveler son pass
 *   ADMIN → suspendre, réactiver, voir tous les pass (via userId)
 *
 * Le débit est appelé exclusivement par billing-service (interne, rôle ADMIN).
 *
 * Sécurité : Gateway injecte X-User-Id (UUID) et X-User-Role.
 */
@RestController
@RequestMapping("/api/users/{id}/pass")
public class PassMobilityController {

    private final PassMobilityService passMobilityService;

    public PassMobilityController(PassMobilityService passMobilityService) {
        this.passMobilityService = passMobilityService;
    }

    // GET /api/users/{id}/pass — consulter son solde (USER son propre, ADMIN tous)
    @GetMapping
    public ResponseEntity<?> obtenirPass(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrAdmin(id, currentUserId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.obtenirPass(id));
    }

    // PUT /pass/suspendre — ADMIN uniquement (dashboard)
    @PutMapping("/suspendre")
    public ResponseEntity<?> suspendrePass(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        return ResponseEntity.ok(passMobilityService.suspendrePass(id));
    }

    // PUT /pass/activer — ADMIN uniquement (dashboard)
    @PutMapping("/activer")
    public ResponseEntity<?> activerPass(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        return ResponseEntity.ok(passMobilityService.activerPass(id));
    }

    // PUT /pass/renouveler — USER (son propre) ou ADMIN
    @PutMapping("/renouveler")
    public ResponseEntity<?> renouvellerPass(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrAdmin(id, currentUserId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.renouvellerPass(id));
    }

    // PUT /pass/debiter — billing-service uniquement (envoie X-User-Role: ADMIN)
    @PutMapping("/debiter")
    public ResponseEntity<?> debiterSolde(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateSoldeRequest request) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.debiterSolde(id, request));
    }

    // PUT /pass/recharger — USER (son propre) ou ADMIN
    @PutMapping("/recharger")
    public ResponseEntity<?> rechargerSolde(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateSoldeRequest request) {

        if (!isOwnerOrAdmin(id, currentUserId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(passMobilityService.rechargerSolde(id, request));
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private boolean isOwnerOrAdmin(UUID resourceId, String currentUserId, String userRole) {
        try {
            return resourceId.equals(UUID.fromString(currentUserId)) || isAdmin(userRole);
        } catch (IllegalArgumentException e) {
            return isAdmin(userRole);
        }
    }

    private boolean isAdmin(String userRole) {
        return "ADMIN".equalsIgnoreCase(userRole);
    }
}
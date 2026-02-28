package com.smartmobility.userservice.controller;

import com.smartmobility.userservice.dto.*;
import com.smartmobility.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController — gestion du profil utilisateur uniquement.
 *
 * Délègue à : UserService
 *
 * Endpoints :
 *   GET    /api/users          → liste tous (MANAGER / ADMIN)
 *   GET    /api/users/{id}     → voir un profil
 *   PUT    /api/users/{id}     → modifier un profil
 *   DELETE /api/users/{id}     → supprimer (ADMIN)
 *
 * Sécurité assurée par la Gateway → headers X-User-Id / X-User-Role injectés.
 * Pas de Spring Security ici.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users — ADMIN uniquement (dashboard)
    @GetMapping
    public ResponseEntity<?> listerUtilisateurs(
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        return ResponseEntity.ok(userService.obtenirTousLesUtilisateurs());
    }

    // GET /api/users/{id} — son propre profil OU ADMIN
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenirUtilisateur(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrAdmin(id, userId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(userService.obtenirUtilisateur(id));
    }

    // PUT /api/users/{id} — son propre profil OU ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<?> mettreAJourUtilisateur(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateUserRequest request) {

        if (!isOwnerOrAdmin(id, userId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(userService.mettreAJourUtilisateur(id, request));
    }

    // DELETE /api/users/{id} — ADMIN uniquement
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerUtilisateur(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        userService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────
    private boolean isOwnerOrAdmin(Long resourceId, String userId, String userRole) {
        return String.valueOf(resourceId).equals(userId) || isAdmin(userRole);
    }

    private boolean isAdmin(String userRole) {
        return "ADMIN".equalsIgnoreCase(userRole);
    }
}
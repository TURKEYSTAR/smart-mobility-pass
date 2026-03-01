package com.smartmobility.userservice.controller;

import com.smartmobility.userservice.dto.UpdateUserRequest;
import com.smartmobility.userservice.dto.UserResponse;
import com.smartmobility.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * UserController — gestion du profil utilisateur.
 *
 * Rôles :
 *   USER  → consulter et modifier son propre profil
 *   ADMIN → consulter tous les profils, supprimer un utilisateur
 *
 * Sécurité : Gateway injecte X-User-Id (UUID) et X-User-Role dans chaque requête.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users — liste tous les utilisateurs (ADMIN dashboard)
    @GetMapping
    public ResponseEntity<?> listerUtilisateurs(
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        List<UserResponse> users = userService.obtenirTousLesUtilisateurs();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/{id} — voir un profil (son propre ou ADMIN)
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenirUtilisateur(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrAdmin(id, currentUserId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(userService.obtenirUtilisateur(id));
    }

    // PUT /api/users/{id} — modifier son profil (son propre ou ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<?> mettreAJourUtilisateur(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id")   String currentUserId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateUserRequest request) {

        if (!isOwnerOrAdmin(id, currentUserId, userRole)) {
            return ResponseEntity.status(403).body("Accès refusé");
        }
        return ResponseEntity.ok(userService.mettreAJourUtilisateur(id, request));
    }

    // DELETE /api/users/{id} — supprimer un utilisateur (ADMIN uniquement)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerUtilisateur(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isAdmin(userRole)) {
            return ResponseEntity.status(403).body("Accès refusé : réservé aux ADMIN");
        }
        userService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /**
     * Vérifie si l'utilisateur courant est le propriétaire de la ressource ou ADMIN.
     * X-User-Id injecté par la Gateway est un UUID sous forme de String.
     */
    private boolean isOwnerOrAdmin(UUID resourceId, String currentUserId, String userRole) {
        try {
            return resourceId.equals(UUID.fromString(currentUserId)) || isAdmin(userRole);
        } catch (IllegalArgumentException e) {
            // currentUserId mal formé → accès refusé
            return isAdmin(userRole);
        }
    }

    private boolean isAdmin(String userRole) {
        return "ADMIN".equalsIgnoreCase(userRole);
    }
}
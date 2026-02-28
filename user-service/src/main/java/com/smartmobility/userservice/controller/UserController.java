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

    // ── GET /api/users — liste tous ───────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listerUtilisateurs(
            @RequestHeader("X-User-Role") String userRole) {

        if (!hasRole(userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403)
                    .body("Accès refusé : réservé aux MANAGER et ADMIN");
        }

        List<UserResponse> users = userService.obtenirTousLesUtilisateurs();
        return ResponseEntity.ok(users);
    }

    // ── GET /api/users/{id} ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenirUtilisateur(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole) {

        if (!isOwnerOrRole(id, userId, userRole, "MANAGER", "ADMIN")) {
            return ResponseEntity.status(403)
                    .body("Accès refusé : vous ne pouvez voir que votre propre profil");
        }

        return ResponseEntity.ok(userService.obtenirUtilisateur(id));
    }

    // ── PUT /api/users/{id} ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> mettreAJourUtilisateur(
            @PathVariable Long id,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody UpdateUserRequest request) {

        if (!isOwnerOrRole(id, userId, userRole, "ADMIN")) {
            return ResponseEntity.status(403)
                    .body("Accès refusé : vous ne pouvez modifier que votre propre profil");
        }

        return ResponseEntity.ok(userService.mettreAJourUtilisateur(id, request));
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerUtilisateur(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        if (!hasRole(userRole, "ADMIN")) {
            return ResponseEntity.status(403)
                    .body("Accès refusé : réservé aux ADMIN");
        }

        userService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    /**
     * Retourne true si l'utilisateur connecté est le propriétaire de la ressource
     * OU s'il possède l'un des rôles autorisés.
     */
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
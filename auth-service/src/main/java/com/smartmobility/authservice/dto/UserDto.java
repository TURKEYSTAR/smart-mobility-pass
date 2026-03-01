package com.smartmobility.authservice.dto;

import java.util.UUID;

/**
 * Miroir exact du UserDto retourné par le user-service via Feign.
 * L'ordre des champs DOIT correspondre au record du user-service
 * pour que Jackson désérialise correctement.
 *
 * Champs : id (UUID), nom, prenom, email, password, username, telephone, role, enabled, googleId
 */
public record UserDto(
        UUID id,
        String nom,
        String prenom,
        String email,
        String password,   // hash BCrypt — utilisé pour vérifier le mot de passe au login
        String username,
        String telephone,
        String role,       // "USER" / "ADMIN"
        boolean enabled,
        String googleId    // null si connexion classique uniquement
) {}
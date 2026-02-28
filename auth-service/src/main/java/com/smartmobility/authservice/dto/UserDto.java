package com.smartmobility.authservice.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String password,   // hash BCrypt — utilisé pour vérifier le mot de passe
        String nom,
        String prenom,
        String username,
        String telephone,
        String role,       // "USER" / "MANAGER" / "ADMIN"
        boolean enabled,
        String googleId    // null si connexion classique uniquement
) {}
package com.smartmobility.authservice.dto;

public record CreateUserRequest(
        String nom,
        String prenom,
        String email,
        String password,   // déjà encodé en BCrypt
        String username,
        String telephone,
        String role,       // "USER" par défaut
        boolean enabled,
        String googleId    // null si inscription classique
) {}

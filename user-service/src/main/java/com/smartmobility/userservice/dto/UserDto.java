package com.smartmobility.userservice.dto;

public record UserDto(
        Long id,
        String nom,
        String prenom,
        String email,
        String password,   // hash BCrypt — utilisé pour vérifier le mot de passe
        String username,
        String telephone,
        String role,       // "USER" / "MANAGER" / "ADMIN"
        boolean enabled,
        String googleId    // null si connexion classique uniquement
) {}

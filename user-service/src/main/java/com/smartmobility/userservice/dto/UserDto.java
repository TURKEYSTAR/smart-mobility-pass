package com.smartmobility.userservice.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String nom,
        String prenom,
        String email,
        String password,   // hash BCrypt — utilisé pour vérifier le mot de passe
        String username,
        String telephone,
        String role,
        boolean enabled,
        String googleId    // null si connexion classique uniquement
) {}

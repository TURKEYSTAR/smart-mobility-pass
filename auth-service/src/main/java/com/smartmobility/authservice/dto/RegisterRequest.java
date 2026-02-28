package com.smartmobility.authservice.dto;

public record RegisterRequest(
        String nom,
        String prenom,
        String email,
        String password,
        String username,
        String telephone
) {}
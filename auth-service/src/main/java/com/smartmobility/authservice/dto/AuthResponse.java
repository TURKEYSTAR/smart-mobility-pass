package com.smartmobility.authservice.dto;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String role,
        long expiresIn
) {}

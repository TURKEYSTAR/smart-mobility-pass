package com.smartmobility.authservice.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String email,
        String role,
        long expiresIn
) {}

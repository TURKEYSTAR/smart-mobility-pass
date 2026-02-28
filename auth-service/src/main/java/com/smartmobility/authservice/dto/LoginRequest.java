package com.smartmobility.authservice.dto;

/** POST /api/auth/login */
public record LoginRequest(
        String email,
        String password
) {}

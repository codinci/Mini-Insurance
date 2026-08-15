package com.example.miniinsurance.auth.dto;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String role
) {
}
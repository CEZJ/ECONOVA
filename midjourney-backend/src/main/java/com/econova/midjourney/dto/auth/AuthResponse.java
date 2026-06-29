package com.econova.midjourney.dto.auth;

public record AuthResponse(
        String token,
        String fullName,
        String email,
        String message
) {
}

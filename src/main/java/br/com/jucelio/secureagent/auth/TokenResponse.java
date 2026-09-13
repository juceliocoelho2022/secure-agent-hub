package br.com.jucelio.secureagent.auth;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken
) {}

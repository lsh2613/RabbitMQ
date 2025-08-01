package com.rabbitmqprac.infra.security.jwt;

public record Jwts(
        String accessToken,
        String refreshToken
) {
    public static Jwts of(String accessToken, String refreshToken) {
        return new Jwts(accessToken, refreshToken);
    }
}

package com.rabbitmqprac.infra.security.jwt;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static com.rabbitmqprac.infra.security.jwt.AccessTokenClaimKeys.USER_ID;
import static com.rabbitmqprac.infra.security.jwt.AccessTokenClaimKeys.ROLE;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RefreshTokenClaim implements JwtClaims {
    private final Map<String, ?> claims;

    public static RefreshTokenClaim of(Long userId, String role) {
        Map<String, Object> claims = Map.of(
                USER_ID.getValue(), userId.toString(),
                ROLE.getValue(), role
        );
        return new RefreshTokenClaim(claims);
    }

    @Override
    public Map<String, ?> getClaims() {
        return claims;
    }
}

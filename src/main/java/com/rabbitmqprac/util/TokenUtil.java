package com.rabbitmqprac.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rabbitmqprac.common.constant.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class TokenUtil {
    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.access.expiration}")
    private Long accessTokenExpirationPeriod;
    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenExpirationPeriod;
    @Value("${jwt.access.header}")
    private String accessHeader;
    @Value("${jwt.refresh.header}")
    private String refreshHeader;

    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private static final String BEARER = "Bearer ";

    private final RedisTokenUtil redisTokenUtil;

    public String issueAccessToken(Long memberId) {
        return JWT.create()
                .withSubject(ACCESS_TOKEN_SUBJECT)
                .withClaim("id", memberId)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpirationPeriod))
                .sign(Algorithm.HMAC512(secretKey));
    }

    public String issueRefreshToken(Long memberId) {
        String refreshToken = JWT.create()
                .withSubject(REFRESH_TOKEN_SUBJECT)
                .withClaim("id", memberId)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpirationPeriod))
                .sign(Algorithm.HMAC512(secretKey));
        saveRefreshToken(memberId, refreshToken);
        return refreshToken;
    }

    private void saveRefreshToken(Long memberId, String refreshToken) {
        redisTokenUtil.setRefreshTokenExpire(refreshToken, memberId, Duration.ofDays(refreshTokenExpirationPeriod));
    }

    public String reissueAccessToken(String refreshToken) {
        DecodedJWT decodedJWT;
        try {
            decodedJWT = JWT.require(Algorithm.HMAC512(secretKey)).build().verify(refreshToken);
        } catch (JWTVerificationException e) {
            throw new JWTVerificationException(e.getMessage());
        }

        Long memberId = decodedJWT.getClaim("id").asLong();
        Long value = redisTokenUtil.getMemberId(refreshToken);

        if (memberId == null || value == null) throw new RuntimeException("토큰이 존재하지 않습니다");
        else if (!memberId.equals(value)) throw new RuntimeException("토큰이 유효하지 않습니다");

        return issueAccessToken(memberId);
    }

    public String extractToken(HttpServletRequest request, TokenType tokenType) {
        Optional<String> requestToken = switch (tokenType) {
            case ACCESS_TOKEN -> Optional.ofNullable(request.getHeader(accessHeader))
                    .filter(token -> token.startsWith(BEARER))
                    .map(token -> token.substring(7));
            case REFRESH_TOKEN -> Optional.ofNullable(request.getHeader(refreshHeader))
                    .filter(token -> token.startsWith(BEARER))
                    .map(token -> token.substring(7));
            default -> throw new IllegalStateException("Unexpected value: " + tokenType);
        };

        return requestToken.orElse(null);
    }

    public String extractToken(StompHeaderAccessor accessor, TokenType tokenType) {
        Optional<String> requestToken = switch (tokenType) {
            case ACCESS_TOKEN -> Optional.ofNullable(accessor.getFirstNativeHeader(accessHeader))
                    .filter(token -> token.startsWith(BEARER))
                    .map(token -> token.substring(7));
            case REFRESH_TOKEN -> Optional.ofNullable(accessor.getFirstNativeHeader(refreshHeader))
                    .filter(token -> token.startsWith(BEARER))
                    .map(token -> token.substring(7));
            default -> throw new IllegalStateException("Unexpected value: " + tokenType);
        };

        return requestToken.orElse(null);
    }

    public DecodedJWT decodedJWT(String accessToken) {
        try {
            return JWT.require(Algorithm.HMAC512(secretKey)).build().verify(accessToken);
        } catch (TokenExpiredException e) {
            log.debug("토큰이 만료되었습니다");
            throw new RuntimeException("토큰이 만료되었습니다");
        }
    }

    public Long validateTokenAndGetMemberId(String token) {
        if (token == null || token.isBlank()) {
            log.error("토큰이 존재하지 않습니다");
            throw new RuntimeException("토큰이 존재하지 않습니다");
        }

        DecodedJWT decodedJWT = decodedJWT(token);
        return decodedJWT.getClaim("id").asLong();
    }
}

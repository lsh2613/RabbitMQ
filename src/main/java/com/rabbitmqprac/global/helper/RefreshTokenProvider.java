package com.rabbitmqprac.global.helper;

import com.rabbitmqprac.infra.security.exception.JwtErrorCode;
import com.rabbitmqprac.infra.security.exception.JwtErrorException;
import com.rabbitmqprac.infra.security.jwt.JwtClaims;
import com.rabbitmqprac.infra.security.jwt.JwtProvider;
import com.rabbitmqprac.infra.security.jwt.RefreshTokenClaim;
import com.rabbitmqprac.global.annotation.Helper;
import com.rabbitmqprac.global.annotation.RefreshTokenStrategy;
import com.rabbitmqprac.global.util.DateUtil;
import com.rabbitmqprac.global.util.JwtErrorCodeUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static com.rabbitmqprac.infra.security.jwt.AccessTokenClaimKeys.ROLE;
import static com.rabbitmqprac.infra.security.jwt.AccessTokenClaimKeys.USER_ID;

@Slf4j
@RefreshTokenStrategy
@Helper
public class RefreshTokenProvider implements JwtProvider {
    private final SecretKey secretKey;
    private final Duration tokenExpiration;

    public RefreshTokenProvider(
            @Value("${jwt.secret-key.refresh-token}") String jwtSecretKey,
            @Value("${jwt.expiration-time.refresh-token}") Duration tokenExpiration
    ) {
        final byte[] secretKeyBytes = Base64.getDecoder().decode(jwtSecretKey);
        this.secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
        this.tokenExpiration = tokenExpiration;
    }

    @Override
    public String generateToken(JwtClaims claims) {
        Date now = new Date();

        return Jwts.builder()
                .header().add(createHeader()).and()
                .claims(claims.getClaims())
                .signWith(secretKey)
                .expiration(createExpireDate(now, tokenExpiration.toMillis()))
                .compact();
    }

    @Override
    public JwtClaims getJwtClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return RefreshTokenClaim.of(
                Long.parseLong(claims.get(USER_ID.getValue(), String.class)),
                claims.get(ROLE.getValue(), String.class)
        );
    }

    @Override
    public LocalDateTime getExpiryDate(String token) {
        Claims claims = getClaimsFromToken(token);
        return DateUtil.toLocalDateTime(claims.getExpiration());
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtErrorException e) {
            if (JwtErrorCode.EXPIRED_TOKEN.equals(e.getErrorCode())) return true;
            throw e;
        }
    }

    @Override
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            final JwtErrorCode errorCode = JwtErrorCodeUtil.determineErrorCode(e, JwtErrorCode.FAILED_AUTHENTICATION);

            log.warn("Error code : {}, Error - {},  {}", errorCode, e.getClass(), e.getMessage());
            throw new JwtErrorException(errorCode);
        }
    }

    private Map<String, Object> createHeader() {
        return Map.of("typ", "JWT",
                "alg", "HS256",
                "regDate", System.currentTimeMillis());
    }

    private Date createExpireDate(final Date now, long expirationTime) {
        return new Date(now.getTime() + expirationTime);
    }
}

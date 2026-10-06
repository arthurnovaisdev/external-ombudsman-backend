package com.mbfreire.employee_reporting.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JWTService {

    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(UserDetails userDetails) {

        if (!(userDetails instanceof UserDetailsImpl customUserDetails)) {
            throw new IllegalArgumentException(
                    "Tipo de usuário inválido para a geração do JWT."
            );
        }

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(
                        "role",
                        userDetails.getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority()
                )
                .claim(
                        TOKEN_VERSION_CLAIM,
                        customUserDetails
                                .getUser()
                                .getTokenVersion()
                )
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expirationMs
                        )
                )
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public long extractTokenVersion(String token) {

        Object value = extractClaim(
                token,
                claims -> claims.get(TOKEN_VERSION_CLAIM)
        );

        if (value instanceof Number number) {
            return number.longValue();
        }

        return -1L;
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        if (!(userDetails instanceof UserDetailsImpl customUserDetails)) {
            return false;
        }

        String username = extractUsername(token);

        long tokenVersion =
                extractTokenVersion(token);

        long currentTokenVersion =
                customUserDetails
                        .getUser()
                        .getTokenVersion();

        boolean sameUser =
                username.equals(
                        userDetails.getUsername()
                );

        boolean sameTokenVersion =
                tokenVersion == currentTokenVersion;

        return sameUser
                && sameTokenVersion
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {

        Date expiration = extractClaim(
                token,
                Claims::getExpiration
        );

        return expiration.before(new Date());
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return resolver.apply(claims);
    }
}
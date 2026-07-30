package com.granttrack.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Stateless validator used by every resource service (core, notification, finance).
 * It verifies the HMAC signature and issuer of an access token and extracts the
 * identity claims — no database lookup. Token <em>issuance</em> lives only in
 * auth-service ({@code JwtTokenProvider}); all services share the same secret.
 */
@Slf4j
@Component
public class JwtTokenValidator {

    private final SecretKey signingKey;
    private final String issuer;

    public JwtTokenValidator(
            @Value("${granttrack.security.jwt.secret}") String secret,
            @Value("${granttrack.security.jwt.issuer:granttrack}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getEmail(String token) {
        return parse(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.granttrack.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /**
     * The well-known development secret shipped in {@code application-local.yml}. It must never be
     * used outside the {@code local} profile; startup fails fast if it is detected elsewhere.
     */
    static final String DEV_SECRET = "Y3JhbnRUcmFja1N1cGVyU2VjcmV0S2V5Rm9ySldUU2lnbmluZzEyMzQ1Njc4OTA=";

    private final JwtProperties properties;
    private final Environment environment;
    private SecretKey signingKey;

    /**
     * Validate the signing secret on startup. In any profile other than {@code local} the secret
     * must be explicitly provided (via {@code GRANTTRACK_JWT_SECRET}) and must not be the committed
     * development value — otherwise tokens could be forged with a publicly known key.
     */
    @PostConstruct
    void validateSecret() {
        boolean localProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");
        String secret = properties.getSecret();
        if (localProfile) {
            return; // Local dev may rely on the bundled development secret.
        }
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "granttrack.security.jwt.secret is not set. Provide a Base64-encoded 256-bit key "
                            + "via the GRANTTRACK_JWT_SECRET environment variable in non-local profiles.");
        }
        if (DEV_SECRET.equals(secret.trim())) {
            throw new IllegalStateException(
                    "Refusing to start with the built-in development JWT secret outside the 'local' profile. "
                            + "Set GRANTTRACK_JWT_SECRET to a unique Base64-encoded 256-bit key.");
        }
    }

    private SecretKey key() {
        if (signingKey == null) {
            signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
        }
        return signingKey;
    }

    public String generateAccessToken(Long userId, String email, Set<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getAccessTokenExpirationMs());
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
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

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

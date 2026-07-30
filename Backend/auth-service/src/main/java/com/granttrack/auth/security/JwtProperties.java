package com.granttrack.auth.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Binds {@code granttrack.security.jwt.*} configuration. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "granttrack.security.jwt")
public class JwtProperties {

    private String secret;

    private long accessTokenExpirationMs = 900_000L;

    private long refreshTokenExpirationMs = 604_800_000L;

    private String issuer = "granttrack";
}

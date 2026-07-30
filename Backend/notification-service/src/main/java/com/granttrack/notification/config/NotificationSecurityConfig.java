package com.granttrack.notification.config;

import com.granttrack.common.security.JwtClaimsAuthenticationFilter;
import com.granttrack.common.security.JwtTokenValidator;
import com.granttrack.common.security.RestAccessDeniedHandler;
import com.granttrack.common.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless resource-server security. User-facing {@code /api/v1/notifications/**} require a
 * valid JWT (claims-based). The service-to-service {@code /internal/**} API is not JWT-gated —
 * it is authenticated by a shared internal token checked in the controller and is never routed
 * publicly by the gateway.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class NotificationSecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/internal/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };

    private final JwtTokenValidator jwtTokenValidator;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtClaimsAuthenticationFilter(jwtTokenValidator),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

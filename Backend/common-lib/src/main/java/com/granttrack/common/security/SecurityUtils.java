package com.granttrack.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof IdentifiableUser user) {
            return Optional.ofNullable(user.getId());
        }
        return Optional.empty();
    }

    /** True if the current authentication holds any of the given authority names (e.g. {@code ROLE_ADMIN}). */
    public static boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            for (String role : roles) {
                if (granted.getAuthority().equals(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}

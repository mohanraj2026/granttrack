package com.granttrack.common.security;

/**
 * Lightweight authenticated principal used by resource services. Built purely from
 * JWT claims (no {@code UserDetails}, no DB row). Implements {@link IdentifiableUser}
 * so {@link SecurityUtils#getCurrentUserId()} keeps working unchanged.
 *
 * @param id    the authenticated user id (JWT subject)
 * @param email the user's email (JWT {@code email} claim)
 */
public record ResourcePrincipal(Long id, String email) implements IdentifiableUser {

    @Override
    public Long getId() {
        return id;
    }
}

package com.granttrack.common.security;

/**
 * Implemented by the authenticated principal so cross-cutting concerns
 * (JPA auditing, audit log) can resolve the current user id without
 * depending on the auth module's concrete {@code UserDetails} type.
 */
public interface IdentifiableUser {
    Long getId();
}

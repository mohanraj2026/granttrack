package com.granttrack.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as a security/decision-sensitive action that must be
 * written to the domain {@code audit_logs} table (award decisions, disbursements,
 * review submissions, etc.). Processed by {@link AuditAspect}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Verb recorded in {@code audit_logs.action}, e.g. "RELEASE_FUNDS". */
    String action();

    /** Entity type recorded in {@code audit_logs.entity_type}, e.g. "GrantAward". */
    String entityType();
}

package com.granttrack.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only domain audit trail for security/decision-sensitive actions.
 * Deliberately does NOT extend {@link com.granttrack.common.entity.BaseEntity}:
 * audit logs are immutable and never soft-deleted or versioned.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "ix_audit_user", columnList = "user_id"),
        @Index(name = "ix_audit_entity", columnList = "entity_type,record_id"),
        @Index(name = "ix_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}

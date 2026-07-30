package com.granttrack.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Persists domain audit-log entries. Runs in its own transaction so an audit
 *  write neither participates in nor rolls back the business transaction. */
@Service
@RequiredArgsConstructor
public class AuditRecorderImpl implements AuditRecorder {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String action, String entityType, Long recordId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .recordId(recordId)
                .details(details)
                .timestamp(Instant.now())
                .build());
    }
}

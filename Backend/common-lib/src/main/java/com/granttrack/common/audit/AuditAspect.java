package com.granttrack.common.audit;

import com.granttrack.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Writes an {@code audit_logs} entry after any {@link Auditable}-annotated service
 * method completes successfully. The record id is best-effort resolved from the
 * returned object's {@code getId()} when present.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditRecorder auditRecorder;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result = pjp.proceed();
        try {
            Long userId = SecurityUtils.getCurrentUserId().orElse(null);
            Long recordId = extractId(result);
            auditRecorder.record(userId, auditable.action(), auditable.entityType(), recordId, null);
        } catch (Exception ex) {
            // Auditing must never break the business operation.
            log.warn("Failed to write audit log for action {}", auditable.action(), ex);
        }
        return result;
    }

    private Long extractId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            var m = result.getClass().getMethod("getId");
            Object value = m.invoke(result);
            return (value instanceof Long id) ? id : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}

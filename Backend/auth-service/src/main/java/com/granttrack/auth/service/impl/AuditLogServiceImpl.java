package com.granttrack.auth.service.impl;

import com.granttrack.auth.dto.response.AuditLogResponse;
import com.granttrack.common.audit.AuditLog;
import com.granttrack.common.audit.AuditLogRepository;
import com.granttrack.auth.service.AuditLogService;
import com.granttrack.common.exception.BusinessException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Read-only, query-side view of the immutable {@code audit_logs} table. No mutation is exposed. */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(Long userId, String entityType, String action, Long recordId,
                                         String from, String to, Pageable pageable) {
        Instant fromInstant = parseInstant(from, "from");
        Instant toInstant = parseInstant(to, "to");
        Specification<AuditLog> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (StringUtils.hasText(entityType)) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (recordId != null) {
                predicates.add(cb.equal(root.get("recordId"), recordId));
            }
            if (fromInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), fromInstant));
            }
            if (toInstant != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), toInstant));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private Instant parseInstant(String raw, String field) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Invalid " + field + " timestamp (expected ISO-8601): " + raw);
        }
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .recordId(log.getRecordId())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}

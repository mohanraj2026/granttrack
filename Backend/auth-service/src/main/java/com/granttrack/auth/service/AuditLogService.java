package com.granttrack.auth.service;

import com.granttrack.auth.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Read-only access to the append-only domain audit trail (Compliance / Admin oversight). */
public interface AuditLogService {

    /**
     * Query the audit trail with optional filters.
     *
     * @param userId     acting user id (optional)
     * @param entityType affected entity type, e.g. "GrantAward" (optional, exact match)
     * @param action     verb, e.g. "RELEASE_FUNDS" (optional, exact match)
     * @param recordId   affected record id (optional)
     * @param from       ISO-8601 instant lower bound on timestamp, inclusive (optional)
     * @param to         ISO-8601 instant upper bound on timestamp, inclusive (optional)
     */
    Page<AuditLogResponse> search(Long userId, String entityType, String action, Long recordId,
                                  String from, String to, Pageable pageable);
}

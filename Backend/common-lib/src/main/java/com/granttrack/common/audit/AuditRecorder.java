package com.granttrack.common.audit;

/**
 * Decouples the {@link AuditAspect} (common) from the auth module's audit-log
 * persistence. Implemented by the auth module.
 */
public interface AuditRecorder {

    /**
     * Persist an audit-log entry.
     *
     * @param userId     acting user id (nullable for system actions)
     * @param action     verb, e.g. "RELEASE_FUNDS"
     * @param entityType affected entity type, e.g. "GrantAward"
     * @param recordId   affected record id (nullable)
     * @param details    optional human/JSON detail
     */
    void record(Long userId, String action, String entityType, Long recordId, String details);
}

package com.granttrack.auth.controller;

import com.granttrack.auth.dto.response.AuditLogResponse;
import com.granttrack.auth.service.AuditLogService;
import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only oversight view of the append-only audit trail. Restricted to the Compliance Officer
 * (the watchdog) and the platform Admin. The trail itself is written by {@code AuditAspect};
 * this controller never mutates it.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log", description = "Read-only oversight of security/decision-sensitive actions")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','ADMIN')")
    @Operation(summary = "Search the audit trail (filter by user, entity type, action, record, date range; paginated, newest first)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(auditLogService.search(userId, entityType, action, recordId, from, to, pageable))));
    }
}

package com.granttrack.notification.controller;

import com.granttrack.notification.dto.request.NotificationRequest;
import com.granttrack.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service publish API. Other services (core) call this via Feign to fire a
 * notification for a user. Authenticated by a shared internal token (not the user JWT),
 * and never routed publicly by the gateway (it lives under {@code /internal}, outside the
 * gateway's {@code /api/v1/notifications/**} route).
 */
@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications (internal)", description = "Service-to-service notification publishing")
public class InternalNotificationController {

    private final NotificationService notificationService;

    @Value("${granttrack.internal.token:}")
    private String internalToken;

    @PostMapping
    @Operation(summary = "Publish a notification (internal, token-authenticated)")
    public ResponseEntity<Void> publish(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody NotificationRequest request) {
        if (!StringUtils.hasText(internalToken) || !internalToken.equals(token)) {
            log.warn("Rejected internal notification publish: invalid or missing internal token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        notificationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

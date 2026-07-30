package com.granttrack.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client to notification-service's internal publish API. Resolved through Eureka
 * ({@code lb://notification-service}); the shared internal token authenticates the call.
 */
@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/internal/notifications")
    void publish(@RequestHeader("X-Internal-Token") String internalToken,
                 @RequestBody NotificationPublishRequest request);
}

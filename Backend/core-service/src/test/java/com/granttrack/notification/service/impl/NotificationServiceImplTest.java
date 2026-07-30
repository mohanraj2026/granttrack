package com.granttrack.notification.service.impl;

import com.granttrack.notification.client.NotificationClient;
import com.granttrack.notification.client.NotificationPublishRequest;
import com.granttrack.notification.entity.NotificationCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "internalToken", "test-token");
    }

    @Test
    void notify_forwardsToClientWithTokenAndPayload() {
        service.notify(42L, "Your award is active", NotificationCategory.AWARD);

        ArgumentCaptor<NotificationPublishRequest> body = ArgumentCaptor.forClass(NotificationPublishRequest.class);
        verify(notificationClient).publish(eq("test-token"), body.capture());
        assertThat(body.getValue().userId()).isEqualTo(42L);
        assertThat(body.getValue().message()).isEqualTo("Your award is active");
        assertThat(body.getValue().category()).isEqualTo("AWARD");
    }

    @Test
    void notify_swallowsDeliveryFailures() {
        doThrow(new RuntimeException("notification-service unavailable"))
                .when(notificationClient).publish(any(), any());

        // A notification-service outage must never break the calling business transaction.
        assertThatCode(() -> service.notify(1L, "msg", NotificationCategory.REVIEW))
                .doesNotThrowAnyException();
    }
}

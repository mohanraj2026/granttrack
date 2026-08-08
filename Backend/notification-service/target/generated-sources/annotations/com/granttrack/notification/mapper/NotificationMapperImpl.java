package com.granttrack.notification.mapper;

import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.entity.Notification;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-08T18:33:02+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse.NotificationResponseBuilder notificationResponse = NotificationResponse.builder();

        notificationResponse.createdAt( notification.getCreatedAt() );
        notificationResponse.id( notification.getId() );
        notificationResponse.message( notification.getMessage() );
        notificationResponse.updatedAt( notification.getUpdatedAt() );
        notificationResponse.userId( notification.getUserId() );

        notificationResponse.category( notification.getCategory().name() );
        notificationResponse.status( notification.getStatus().name() );

        return notificationResponse.build();
    }
}

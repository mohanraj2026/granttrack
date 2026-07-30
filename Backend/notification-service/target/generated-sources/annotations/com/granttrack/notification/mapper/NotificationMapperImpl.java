package com.granttrack.notification.mapper;

import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.entity.Notification;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-30T11:27:11+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse.NotificationResponseBuilder notificationResponse = NotificationResponse.builder();

        notificationResponse.id( notification.getId() );
        notificationResponse.userId( notification.getUserId() );
        notificationResponse.message( notification.getMessage() );
        notificationResponse.createdAt( notification.getCreatedAt() );
        notificationResponse.updatedAt( notification.getUpdatedAt() );

        notificationResponse.category( notification.getCategory().name() );
        notificationResponse.status( notification.getStatus().name() );

        return notificationResponse.build();
    }
}

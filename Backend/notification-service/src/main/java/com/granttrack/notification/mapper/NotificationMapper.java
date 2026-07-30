package com.granttrack.notification.mapper;

import com.granttrack.notification.dto.response.NotificationResponse;
import com.granttrack.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "category", expression = "java(notification.getCategory().name())")
    @Mapping(target = "status", expression = "java(notification.getStatus().name())")
    NotificationResponse toResponse(Notification notification);
}

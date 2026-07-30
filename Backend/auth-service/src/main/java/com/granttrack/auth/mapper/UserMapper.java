package com.granttrack.auth.mapper;

import com.granttrack.auth.entity.Role;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

/** Maps {@link User} entities to response DTOs. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @org.mapstruct.Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    @org.mapstruct.Mapping(target = "status", expression = "java(user.getStatus().name())")
    UserResponse toResponse(User user);

    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        return roles == null ? Set.of() : roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}

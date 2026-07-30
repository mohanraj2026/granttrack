package com.granttrack.auth.mapper;

import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-30T16:08:52+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.roles( rolesToNames( user.getRoles() ) );
        userResponse.collegeIdPath( user.getCollegeIdPath() );
        userResponse.countryCode( user.getCountryCode() );
        userResponse.createdAt( user.getCreatedAt() );
        userResponse.department( user.getDepartment() );
        userResponse.education( user.getEducation() );
        userResponse.email( user.getEmail() );
        userResponse.id( user.getId() );
        userResponse.institutionId( user.getInstitutionId() );
        userResponse.name( user.getName() );
        userResponse.phone( user.getPhone() );
        userResponse.profilePhotoPath( user.getProfilePhotoPath() );
        userResponse.updatedAt( user.getUpdatedAt() );

        userResponse.status( user.getStatus().name() );

        return userResponse.build();
    }
}

package com.granttrack.auth.mapper;

import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-10T17:04:25+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.2 (Oracle Corporation)"
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
        userResponse.id( user.getId() );
        userResponse.name( user.getName() );
        userResponse.email( user.getEmail() );
        userResponse.phone( user.getPhone() );
        userResponse.countryCode( user.getCountryCode() );
        userResponse.institutionId( user.getInstitutionId() );
        userResponse.department( user.getDepartment() );
        userResponse.education( user.getEducation() );
        userResponse.collegeIdPath( user.getCollegeIdPath() );
        userResponse.profilePhotoPath( user.getProfilePhotoPath() );
        userResponse.createdAt( user.getCreatedAt() );
        userResponse.updatedAt( user.getUpdatedAt() );

        userResponse.status( user.getStatus().name() );

        return userResponse.build();
    }
}

package com.granttrack.user.service.impl;

import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.entity.Role;
import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.entity.UserStatus;
import com.granttrack.auth.mapper.UserMapper;
import com.granttrack.auth.repository.RefreshTokenRepository;
import com.granttrack.auth.repository.RoleRepository;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.user.dto.request.AdminCreateUserRequest;
import com.granttrack.user.dto.request.AdminUpdateUserRequest;
import com.granttrack.user.dto.response.CreatedUserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @SuppressWarnings("unchecked")
    void search_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder().email("test@test.com").build();
        user.setId(1L);
        UserResponse response = UserResponse.builder().id(1L).email("test@test.com").build();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        Page<UserResponse> result = userService.search("test", "ACTIVE", null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test@test.com", result.getContent().get(0).email());
        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void lookupResearchers_Success() {
        // Arrange
        User user = User.builder().email("res@test.com").build();
        user.setId(1L);
        UserResponse response = UserResponse.builder().id(1L).email("res@test.com").build();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        List<UserResponse> result = userService.lookupResearchers("res");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("res@test.com", result.get(0).email());
    }

    @Test
    void getById_Success() {
        // Arrange
        Long userId = 1L;
        User user = User.builder().email("test@test.com").build();
        user.setId(userId);
        UserResponse response = UserResponse.builder().id(userId).email("test@test.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        UserResponse result = userService.getById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.id());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(1L));
    }

    @Test
    void setStatus_Success() {
        // Arrange
        Long userId = 1L;
        User user = User.builder().status(UserStatus.ACTIVE).build();
        user.setId(userId);
        UserResponse response = UserResponse.builder().id(userId).status("INACTIVE").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        UserResponse result = userService.setStatus(userId, "INACTIVE");

        // Assert
        assertNotNull(result);
        assertEquals("INACTIVE", result.status());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        verify(userRepository, times(1)).save(user);
        // Deactivation must revoke all refresh tokens so the user cannot mint new access tokens.
        verify(refreshTokenRepository, times(1)).revokeAllForUser(userId);
    }

    @Test
    void setStatus_Reactivate_DoesNotRevokeTokens() {
        // Arrange
        Long userId = 1L;
        User user = User.builder().status(UserStatus.INACTIVE).build();
        user.setId(userId);
        UserResponse response = UserResponse.builder().id(userId).status("ACTIVE").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        userService.setStatus(userId, "ACTIVE");

        // Assert
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(refreshTokenRepository, never()).revokeAllForUser(anyLong());
    }

    @Test
    void setStatus_InvalidValue_ThrowsBusinessException() {
        // Arrange
        Long userId = 1L;
        User user = User.builder().status(UserStatus.ACTIVE).build();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert — an unknown status must be a clean 400, not a 500.
        assertThrows(BusinessException.class, () -> userService.setStatus(userId, "BOGUS"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_AdminCreatesReviewer_Success() {
        // Arrange
        AdminCreateUserRequest request = new AdminCreateUserRequest("Reviewer", "rev@test.com", "123", "password123", 1L, "Dept", "ROLE_REVIEWER");
        Role role = Role.builder().name(RoleName.ROLE_REVIEWER.name()).build();
        role.setId(1L);
        User savedUser = User.builder().email(request.email()).build();
        savedUser.setId(2L);
        UserResponse response = UserResponse.builder().id(2L).email(request.email()).build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_REVIEWER.name())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        // Act
        CreatedUserResponse result = userService.createUser(request, Set.of(RoleName.ROLE_ADMIN.name()));

        // Assert
        assertNotNull(result);
        assertEquals(request.email(), result.user().email());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_GrantAdminCreatesGrantAdmin_ThrowsException() {
        // Arrange
        AdminCreateUserRequest request = new AdminCreateUserRequest("Admin2", "admin@test.com", "123", "password123", 1L, "Dept", "ROLE_GRANT_ADMIN");
        
        // Act & Assert
        // Grant Admin cannot create another Grant Admin
        assertThrows(BusinessException.class, () -> userService.createUser(request, Set.of(RoleName.ROLE_GRANT_ADMIN.name())));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        // Arrange
        AdminCreateUserRequest request = new AdminCreateUserRequest("Reviewer", "rev@test.com", "123", "password123", 1L, "Dept", "ROLE_REVIEWER");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request, Set.of(RoleName.ROLE_ADMIN.name())));
    }

    @Test
    void updateUser_AdminUpdatesResearcher_Success() {
        // Arrange
        Long userId = 1L;
        Role researcherRole = Role.builder().name(RoleName.ROLE_RESEARCHER.name()).build();
        User user = User.builder().name("Old Name").email("old@test.com").roles(Set.of(researcherRole)).build();
        user.setId(userId);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest("New Name", "new@test.com", "999", 5L, "Physics");
        UserResponse response = UserResponse.builder().id(userId).name("New Name").email("new@test.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        UserResponse result = userService.updateUser(userId, request, Set.of(RoleName.ROLE_ADMIN.name()));

        // Assert
        assertEquals("New Name", user.getName());
        assertEquals("new@test.com", user.getEmail());
        assertEquals("999", user.getPhone());
        assertEquals(5L, user.getInstitutionId());
        assertEquals("Physics", user.getDepartment());
        assertEquals("new@test.com", result.email());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUser_SameEmail_SkipsDuplicateCheck() {
        // Arrange — keeping the same email must NOT trigger a duplicate-email rejection.
        Long userId = 1L;
        Role researcherRole = Role.builder().name(RoleName.ROLE_RESEARCHER.name()).build();
        User user = User.builder().name("Ada").email("ada@test.com").roles(Set.of(researcherRole)).build();
        user.setId(userId);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest("Ada R", "ada@test.com", null, null, null);
        UserResponse response = UserResponse.builder().id(userId).email("ada@test.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        // Act
        userService.updateUser(userId, request, Set.of(RoleName.ROLE_ADMIN.name()));

        // Assert — existsByEmail is never consulted when the email is unchanged.
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsException() {
        // Arrange
        Long userId = 1L;
        Role researcherRole = Role.builder().name(RoleName.ROLE_RESEARCHER.name()).build();
        User user = User.builder().email("old@test.com").roles(Set.of(researcherRole)).build();
        user.setId(userId);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest("Name", "taken@test.com", null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class,
                () -> userService.updateUser(userId, request, Set.of(RoleName.ROLE_ADMIN.name())));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_GrantAdminEditingFinanceOfficer_ThrowsException() {
        // Arrange — a Grant Admin may not edit a Finance Officer (separation of duties).
        Long userId = 1L;
        Role foRole = Role.builder().name(RoleName.ROLE_FINANCE_OFFICER.name()).build();
        User user = User.builder().email("fo@test.com").roles(Set.of(foRole)).build();
        user.setId(userId);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest("FO", "fo@test.com", null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> userService.updateUser(userId, request, Set.of(RoleName.ROLE_GRANT_ADMIN.name())));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_Success() {
        // Arrange
        Long userId = 1L;
        Role researcherRole = Role.builder().name(RoleName.ROLE_RESEARCHER.name()).build();
        User user = User.builder().roles(Set.of(researcherRole)).build();
        user.setId(userId);
        user.setDeleted(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.delete(userId, Set.of(RoleName.ROLE_ADMIN.name()));

        // Assert
        assertTrue(user.isDeleted());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void delete_AdminTryingToDeleteAdmin_ThrowsException() {
        // Arrange
        Long userId = 1L;
        Role adminRole = Role.builder().name(RoleName.ROLE_ADMIN.name()).build();
        User user = User.builder().roles(Set.of(adminRole)).build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BusinessException.class, () -> userService.delete(userId, Set.of(RoleName.ROLE_ADMIN.name())));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void delete_GrantAdminTryingToDeleteFinanceOfficer_ThrowsException() {
        // Arrange
        Long userId = 1L;
        Role foRole = Role.builder().name(RoleName.ROLE_FINANCE_OFFICER.name()).build();
        User user = User.builder().roles(Set.of(foRole)).build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        // GRANT_ADMIN is not in the allowed set to delete FINANCE_OFFICER
        assertThrows(BusinessException.class, () -> userService.delete(userId, Set.of(RoleName.ROLE_GRANT_ADMIN.name())));
        verify(userRepository, never()).save(any(User.class));
    }
}

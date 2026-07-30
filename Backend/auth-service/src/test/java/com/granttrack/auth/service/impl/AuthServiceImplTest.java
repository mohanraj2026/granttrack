package com.granttrack.auth.service.impl;

import com.granttrack.auth.service.DocumentStorageService;
import com.granttrack.auth.dto.request.ChangePasswordRequest;
import com.granttrack.auth.dto.request.ForgotPasswordRequest;
import com.granttrack.auth.dto.request.LoginRequest;
import com.granttrack.auth.dto.request.RefreshTokenRequest;
import com.granttrack.auth.dto.request.RegisterRequest;
import com.granttrack.auth.dto.response.AuthResponse;
import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.entity.RefreshToken;
import com.granttrack.auth.entity.Role;
import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.mapper.UserMapper;
import com.granttrack.auth.repository.RefreshTokenRepository;
import com.granttrack.auth.repository.RoleRepository;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.auth.security.CustomUserDetails;
import com.granttrack.auth.security.JwtProperties;
import com.granttrack.auth.security.JwtTokenProvider;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private UserMapper userMapper;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User", "test@test.com", "password", "12345", "+1", 1L, "Dept", "PhD");
        Role role = Role.builder().name(RoleName.ROLE_RESEARCHER.name()).build();
        role.setId(1L);
        User savedUser = User.builder().email(request.email()).build();
        savedUser.setId(1L);
        UserResponse userResponse = UserResponse.builder().id(1L).email(request.email()).build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_RESEARCHER.name())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        // Act
        UserResponse response = authService.register(request, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(request.email(), response.email());
        verify(userRepository, times(1)).save(any(User.class));
        verify(documentStorageService, never()).storeCollegeId(any(), any());
    }

    @Test
    void register_WithFiles_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User", "test@test.com", "password", "12345", "+1", 1L, "Dept", "PhD");
        Role role = Role.builder().name(RoleName.ROLE_RESEARCHER.name()).build();
        role.setId(1L);
        User savedUser = User.builder().email(request.email()).build();
        savedUser.setId(1L);
        UserResponse userResponse = UserResponse.builder().id(1L).email(request.email()).build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_RESEARCHER.name())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);
        when(mockFile.isEmpty()).thenReturn(false);
        when(documentStorageService.storeCollegeId(anyLong(), any())).thenReturn("path/collegeId");
        when(documentStorageService.storeProfilePhoto(anyLong(), any())).thenReturn("path/profilePhoto");

        // Act
        UserResponse response = authService.register(request, mockFile, mockFile);

        // Assert
        assertNotNull(response);
        // Saved twice: once for initial save, once after updating paths
        verify(userRepository, times(2)).save(any(User.class));
        verify(documentStorageService, times(1)).storeCollegeId(anyLong(), any());
        verify(documentStorageService, times(1)).storeProfilePhoto(anyLong(), any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test User", "test@test.com", "password", "12345", "+1", 1L, "Dept", "PhD");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(request, null, null));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest("test@test.com", "password");
        User user = User.builder().email("test@test.com").roles(Set.of(Role.builder().name("ROLE_RESEARCHER").build())).build();
        user.setId(1L);
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        UserResponse userResponse = UserResponse.builder().id(1L).email(request.email()).build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateAccessToken(anyLong(), anyString(), anySet())).thenReturn("access-token");
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(86400000L);
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_Success() {
        // Arrange
        String oldToken = "old-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(oldToken);
        RefreshToken storedToken = RefreshToken.builder().userId(1L).token(oldToken).revoked(false).expiryDate(java.time.Instant.now().plus(java.time.Duration.ofDays(1))).build();
        User user = User.builder().email("test@test.com").roles(Set.of(Role.builder().name("ROLE_RESEARCHER").build())).build();
        user.setId(1L);
        UserResponse userResponse = UserResponse.builder().id(1L).email("test@test.com").build();

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(anyLong(), anyString(), anySet())).thenReturn("new-access-token");
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(86400000L);
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(3600000L);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        // Act
        AuthResponse response = authService.refresh(request);

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertTrue(storedToken.isRevoked()); // old token must be revoked
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // 1 for update, 1 for new token
    }

    @Test
    void refresh_DeactivatedUser_ThrowsAndRevokesTokens() {
        // Arrange — a valid, active refresh token but the owning account has been deactivated.
        String oldToken = "old-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(oldToken);
        RefreshToken storedToken = RefreshToken.builder().userId(1L).token(oldToken).revoked(false)
                .expiryDate(java.time.Instant.now().plus(java.time.Duration.ofDays(1))).build();
        User user = User.builder().email("test@test.com").status(com.granttrack.auth.entity.UserStatus.INACTIVE)
                .roles(Set.of(Role.builder().name("ROLE_RESEARCHER").build())).build();
        user.setId(1L);

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(storedToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.refresh(request));
        verify(refreshTokenRepository, times(1)).revokeAllForUser(1L);
        verify(tokenProvider, never()).generateAccessToken(anyLong(), anyString(), anySet());
    }

    @Test
    void refresh_ExpiredOrRevokedToken_ThrowsException() {
        // Arrange
        String oldToken = "old-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(oldToken);
        RefreshToken storedToken = RefreshToken.builder().userId(1L).token(oldToken).revoked(true).build();

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(storedToken));

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.refresh(request));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void logout_Success() {
        // Arrange
        String tokenVal = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(tokenVal);
        RefreshToken storedToken = RefreshToken.builder().userId(1L).token(tokenVal).revoked(false).build();

        when(refreshTokenRepository.findByToken(tokenVal)).thenReturn(Optional.of(storedToken));

        // Act
        authService.logout(request);

        // Assert
        assertTrue(storedToken.isRevoked());
        verify(refreshTokenRepository, times(1)).save(storedToken);
    }

    @Test
    void changePassword_Success() {
        // Arrange
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");
        User user = User.builder().password("encodedOldPass").build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        // Act
        authService.changePassword(userId, request);

        // Assert
        assertEquals("encodedNewPass", user.getPassword());
        verify(userRepository, times(1)).save(user);
        verify(refreshTokenRepository, times(1)).revokeAllForUser(userId);
    }

    @Test
    void changePassword_IncorrectCurrentPassword_ThrowsException() {
        // Arrange
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass");
        User user = User.builder().password("encodedOldPass").build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedOldPass")).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.changePassword(userId, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void forgotPassword_Success() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@test.com");
        User user = User.builder().email("test@test.com").build();
        user.setId(1L);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        // Act
        authService.forgotPassword(request);

        // Assert
        verify(userRepository, times(1)).findByEmail("test@test.com");
    }
}

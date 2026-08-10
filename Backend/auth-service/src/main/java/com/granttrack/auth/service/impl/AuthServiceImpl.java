package com.granttrack.auth.service.impl;

import com.granttrack.auth.dto.request.ChangePasswordRequest;
import com.granttrack.auth.dto.request.LoginRequest;
import com.granttrack.auth.dto.request.RefreshTokenRequest;
import com.granttrack.auth.dto.request.RegisterRequest;
import com.granttrack.auth.dto.response.AuthResponse;
import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.entity.RefreshToken;
import com.granttrack.auth.entity.Role;
import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.entity.UserStatus;
import com.granttrack.auth.mapper.UserMapper;
import com.granttrack.auth.repository.RefreshTokenRepository;
import com.granttrack.auth.repository.RoleRepository;
import com.granttrack.auth.repository.UserRepository;
import com.granttrack.auth.security.CustomUserDetails;
import com.granttrack.auth.security.JwtProperties;
import com.granttrack.auth.security.JwtTokenProvider;
import com.granttrack.auth.service.AuthService;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    private final com.granttrack.auth.service.DocumentStorageService documentStorageService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request, org.springframework.web.multipart.MultipartFile collegeId, org.springframework.web.multipart.MultipartFile profilePhoto) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        Role researcher = roleRepository.findByName(RoleName.ROLE_RESEARCHER.name())
                .orElseThrow(() -> new BusinessException("Default researcher role is not configured"));
        Set<Role> roles = Set.of(researcher);
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .countryCode(request.countryCode())
                .institutionId(request.institutionId())
                .department(request.department())
                .education(request.education())
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();
        user = userRepository.save(user);

        boolean updated = false;
        if (collegeId != null && !collegeId.isEmpty()) {
            user.setCollegeIdPath(documentStorageService.storeCollegeId(user.getId(), collegeId));
            updated = true;
        }
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            user.setProfilePhotoPath(documentStorageService.storeProfilePhoto(user.getId(), profilePhoto));
            updated = true;
        }
        if (updated) {
            userRepository.save(user);
        }

        log.info("Registered user id={} email={}", user.getId(), user.getEmail());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return issueTokens(principal);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));
        if (!stored.isActive()) {
            throw new BusinessException("Refresh token expired or revoked");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", stored.getUserId()));
        // A deactivated account must not be able to mint fresh access tokens.
        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllForUser(user.getId());
            throw new BusinessException("Account is not active");
        }
        // Rotate: revoke the used token and issue a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(new CustomUserDetails(user));
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId);
        log.info("Password changed for user id={}", userId);
    }

    private AuthResponse issueTokens(CustomUserDetails principal) {
        Set<String> roleNames = principal.getRoleNames();
        String accessToken = tokenProvider.generateAccessToken(principal.getId(), principal.getEmail(), roleNames);
        String refreshTokenValue = UUID.randomUUID().toString() + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(principal.getId())
                .token(refreshTokenValue)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build());
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getAccessTokenExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }
}

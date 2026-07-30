package com.granttrack.auth.service;

import com.granttrack.auth.dto.request.ChangePasswordRequest;
import com.granttrack.auth.dto.request.ForgotPasswordRequest;
import com.granttrack.auth.dto.request.LoginRequest;
import com.granttrack.auth.dto.request.RefreshTokenRequest;
import com.granttrack.auth.dto.request.RegisterRequest;
import com.granttrack.auth.dto.response.AuthResponse;
import com.granttrack.auth.dto.response.UserResponse;

/** Authentication & account use cases (Module 1). */
public interface AuthService {

    UserResponse register(RegisterRequest request, org.springframework.web.multipart.MultipartFile collegeId, org.springframework.web.multipart.MultipartFile profilePhoto);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    /** Phase-1 stub: generates a reset structure (token) without email delivery. */
    void forgotPassword(ForgotPasswordRequest request);
}

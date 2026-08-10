package com.granttrack.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.auth.dto.request.ChangePasswordRequest;
import com.granttrack.auth.dto.request.LoginRequest;
import com.granttrack.auth.dto.request.RefreshTokenRequest;
import com.granttrack.auth.dto.request.RegisterRequest;
import com.granttrack.auth.dto.response.AuthResponse;
import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.security.CustomUserDetails;
import com.granttrack.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean(name = "auditorAware")
    private org.springframework.data.domain.AuditorAware<Long> auditorAware;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.auth.security.JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.granttrack.auth.security.CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "test@test.com", "password123", "1234567890", "+1", 1L, "Dept", "PhD");
        UserResponse response = UserResponse.builder().id(1L).email("test@test.com").name("Test User").build();

        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile collegeIdPart = new MockMultipartFile("collegeId", "id.jpg", "image/jpeg", "image".getBytes());
        
        when(authService.register(any(), any(), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/auth/register")
                        .file(requestPart)
                        .file(collegeIdPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));

        verify(authService, times(1)).register(any(), any(), any());
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        AuthResponse response = AuthResponse.builder().accessToken("token").refreshToken("refresh").tokenType("Bearer").build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void refresh_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        AuthResponse response = AuthResponse.builder().accessToken("new-token").refreshToken("new-refresh").tokenType("Bearer").build();

        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-token"));

        verify(authService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    void logout_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService, times(1)).logout(any(RefreshTokenRequest.class));
    }

    @Test
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPassword123");

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService, times(1)).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }
}


package com.granttrack.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.security.CustomUserDetails;
import com.granttrack.user.dto.request.AdminCreateUserRequest;
import com.granttrack.user.dto.response.CreatedUserResponse;
import com.granttrack.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

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
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void mockPrincipal() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(1L);
        when(principal.getRoleNames()).thenReturn(java.util.Set.of("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, null));
    }

    @Test
    void createUser_Success() throws Exception {
        mockPrincipal();
        AdminCreateUserRequest request = new AdminCreateUserRequest("New Reviewer", "rev@test.com", "123", "password123", 1L, "Dept", "ROLE_REVIEWER");
        UserResponse userResponse = UserResponse.builder().id(2L).email("rev@test.com").build();
        CreatedUserResponse createdUserResponse = CreatedUserResponse.builder().user(userResponse).build();

        when(userService.createUser(any(AdminCreateUserRequest.class), anySet())).thenReturn(createdUserResponse);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("rev@test.com"));

        verify(userService, times(1)).createUser(any(AdminCreateUserRequest.class), anySet());
    }

    @Test
    void search_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder().id(1L).email("test@test.com").build();
        Page<UserResponse> page = new PageImpl<>(List.of(userResponse));

        when(userService.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users")
                        .param("q", "test")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("test@test.com"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(userService, times(1)).search(eq("test"), eq("ACTIVE"), isNull(), any(Pageable.class));
    }

    @Test
    void lookupResearchers_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder().id(1L).email("res@test.com").build();

        when(userService.lookupResearchers(anyString())).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/v1/users/lookup")
                        .param("q", "res")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("res@test.com"));

        verify(userService, times(1)).lookupResearchers("res");
    }

    @Test
    void getById_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder().id(1L).email("test@test.com").build();

        when(userService.getById(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));

        verify(userService, times(1)).getById(1L);
    }

    @Test
    void setStatus_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder().id(1L).status("INACTIVE").build();

        when(userService.setStatus(eq(1L), eq("INACTIVE"))).thenReturn(userResponse);

        mockMvc.perform(patch("/api/v1/users/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(userService, times(1)).setStatus(1L, "INACTIVE");
    }

    @Test
    void delete_Success() throws Exception {
        mockPrincipal();

        mockMvc.perform(delete("/api/v1/users/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService, times(1)).delete(eq(2L), anySet());
    }
}


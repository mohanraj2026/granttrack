package com.granttrack.user.controller;

import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.auth.security.CustomUserDetails;
import com.granttrack.common.dto.ApiResponse;
import com.granttrack.common.dto.PageResponse;
import com.granttrack.user.dto.request.AdminCreateUserRequest;
import com.granttrack.user.dto.request.AdminUpdateUserRequest;
import com.granttrack.user.dto.response.CreatedUserResponse;
import com.granttrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Administration", description = "Search, view and manage user accounts (admin)")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Provision an operational user account (role-gated: only ADMIN may create Finance Officers/Admins)")
    public ResponseEntity<ApiResponse<CreatedUserResponse>> createUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AdminCreateUserRequest request) {
        CreatedUserResponse created = userService.createUser(request, principal.getRoleNames());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User account created. Share the temporary password securely.", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Search/list users with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        var page = userService.search(q, status, role, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lookup researchers by email or name (accessible to all authenticated users)")
    public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> lookupResearchers(
            @RequestParam(required = false) String q) {
        var users = userService.lookupResearchers(q);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Get a user by id")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Update an existing user's editable details")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        UserResponse updated = userService.updateUser(id, request, principal.getRoleNames());
        return ResponseEntity.ok(ApiResponse.success("User updated successfully.", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> setStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("User status updated", userService.setStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GRANT_ADMIN')")
    @Operation(summary = "Delete a user account")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        userService.delete(id, principal.getRoleNames());
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}

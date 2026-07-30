package com.granttrack.user.service;

import com.granttrack.auth.dto.response.UserResponse;
import com.granttrack.user.dto.request.AdminCreateUserRequest;
import com.granttrack.user.dto.request.AdminUpdateUserRequest;
import com.granttrack.user.dto.response.CreatedUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.List;

/** User administration use cases (Module 1 — user management). */
public interface UserService {

    Page<UserResponse> search(String query, String status, String role, Pageable pageable);

    List<UserResponse> lookupResearchers(String query);

    UserResponse getById(Long id);

    UserResponse setStatus(Long id, String status);

    /**
     * Provision an operational user account. The {@code creatorRoles} drive what
     * roles may be granted (separation of duties): only ROLE_ADMIN may create
     * Finance Officers, Grant Admins or other Admins.
     */
    CreatedUserResponse createUser(AdminCreateUserRequest request, Set<String> creatorRoles);

    /**
     * Update an existing user's editable details (name, email, phone, institution, department).
     * {@code actorRoles} enforce separation of duties: a Grant Admin may only edit users whose
     * roles it is allowed to manage (not Finance Officers, Grant Admins or Admins).
     */
    UserResponse updateUser(Long id, AdminUpdateUserRequest request, Set<String> actorRoles);

    void delete(Long id, Set<String> deletorRoles);
}

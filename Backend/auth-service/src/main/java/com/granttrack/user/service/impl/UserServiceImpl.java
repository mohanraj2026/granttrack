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
import com.granttrack.common.audit.Auditable;
import com.granttrack.common.exception.BusinessException;
import com.granttrack.common.exception.DuplicateResourceException;
import com.granttrack.common.exception.ResourceNotFoundException;
import com.granttrack.user.dto.request.AdminCreateUserRequest;
import com.granttrack.user.dto.request.AdminUpdateUserRequest;
import com.granttrack.user.dto.response.CreatedUserResponse;
import com.granttrack.user.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    /** Roles a ROLE_GRANT_ADMIN may provision (NOT Finance Officer / Grant Admin / Admin). */
    private static final Set<String> GRANT_ADMIN_ALLOWED = Set.of(
            RoleName.ROLE_RESEARCHER.name(),
            RoleName.ROLE_REVIEWER.name(),
            RoleName.ROLE_COMPLIANCE_OFFICER.name());

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(String query, String status, String role, Pageable pageable) {
        Specification<User> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query)) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            if (StringUtils.hasText(role)) {
                predicates.add(cb.equal(root.join("roles").get("name"), parseRole(role)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> lookupResearchers(String query) {
        Specification<User> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), UserStatus.ACTIVE));
            
            // Only find researchers
            predicates.add(cb.equal(root.join("roles").get("name"), RoleName.ROLE_RESEARCHER.name()));
            
            if (StringUtils.hasText(query)) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, Pageable.ofSize(20)).getContent().stream()
                .map(userMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_USER_STATUS", entityType = "User")
    public UserResponse setStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        UserStatus newStatus = parseStatus(status);
        user.setStatus(newStatus);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        // Deactivating an account must immediately end its sessions: revoke all refresh tokens so
        // the user cannot mint new access tokens (the JWT filter already blocks disabled accounts).
        if (newStatus == UserStatus.INACTIVE) {
            refreshTokenRepository.revokeAllForUser(id);
            log.info("Deactivated user id={} and revoked all refresh tokens", id);
        }
        return response;
    }

    private UserStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException("Status is required");
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid status: " + status);
        }
    }

    private String parseRole(String role) {
        String normalized = normalizeRole(role);
        try {
            return RoleName.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid role: " + role);
        }
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_USER", entityType = "User")
    public CreatedUserResponse createUser(AdminCreateUserRequest request, Set<String> creatorRoles) {
        String roleName = normalizeRole(request.role());
        validateAssignable(roleName, creatorRoles);

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("Unknown role: " + roleName));

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .institutionId(request.institutionId())
                .department(request.department())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();
        user = userRepository.save(user);

        // Email delivery is deferred (Phase 1): credentials are returned once for secure hand-off.
        log.info("Provisioned {} account id={} email={} (credentials to be delivered to user)",
                roleName, user.getId(), user.getEmail());

        return CreatedUserResponse.builder()
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_USER", entityType = "User")
    public UserResponse updateUser(Long id, AdminUpdateUserRequest request, Set<String> actorRoles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Separation of duties: a Grant Admin may only edit the roles it is allowed to manage
        // (researchers, reviewers, compliance officers) — not finance officers, grant admins or admins.
        boolean isAdmin = actorRoles.contains(RoleName.ROLE_ADMIN.name());
        if (!isAdmin) {
            boolean hasRestrictedRole = user.getRoles().stream()
                    .anyMatch(r -> !GRANT_ADMIN_ALLOWED.contains(r.getName()));
            if (hasRestrictedRole) {
                throw new BusinessException("You do not have permission to edit this user.");
            }
        }

        // Email uniqueness — allow keeping the same email, reject collisions with another account.
        String newEmail = request.email().trim();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Email already registered: " + newEmail);
        }

        user.setName(request.name());
        user.setEmail(newEmail);
        user.setPhone(request.phone());
        user.setDepartment(request.department());
        user.setInstitutionId(request.institutionId());

        User saved = userRepository.save(user);
        log.info("Updated user id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_USER", entityType = "User")
    public void delete(Long id, Set<String> deletorRoles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
                
        // Ensure Admin cannot be deleted
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals(RoleName.ROLE_ADMIN.name()))) {
            throw new BusinessException("Cannot delete the system administrator.");
        }
        
        // Grant Admin cannot delete Finance Officers or Grant Admins or Admins
        boolean isAdmin = deletorRoles.contains(RoleName.ROLE_ADMIN.name());
        if (!isAdmin) {
            boolean hasRestrictedRole = user.getRoles().stream().anyMatch(r -> 
                !GRANT_ADMIN_ALLOWED.contains(r.getName()));
            if (hasRestrictedRole) {
                throw new BusinessException("You do not have permission to delete this user.");
            }
        }

        user.setDeleted(true);
        userRepository.save(user);
        log.info("Deleted user id={}", user.getId());
    }

    private void validateAssignable(String roleName, Set<String> creatorRoles) {
        // Validate it is a real role first.
        try {
            RoleName.valueOf(roleName);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid role: " + roleName);
        }
        boolean isAdmin = creatorRoles.contains(RoleName.ROLE_ADMIN.name());
        if (isAdmin) {
            return; // Main admin may provision any role.
        }
        // Grant Admin: restricted set (cannot create Finance Officers, Grant Admins or Admins).
        if (!GRANT_ADMIN_ALLOWED.contains(roleName)) {
            throw new BusinessException(
                    "Only the main Administrator can create users with role " + roleName);
        }
    }

    private String normalizeRole(String raw) {
        String trimmed = raw.trim().toUpperCase();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

}

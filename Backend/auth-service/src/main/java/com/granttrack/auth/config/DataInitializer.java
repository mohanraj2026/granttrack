package com.granttrack.auth.config;

import com.granttrack.auth.entity.RoleName;
import com.granttrack.auth.entity.User;
import com.granttrack.auth.entity.UserStatus;
import com.granttrack.auth.repository.RoleRepository;
import com.granttrack.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * Bootstraps a default ADMIN account on first run (idempotent) so the system is
 * usable immediately. Override credentials via {@code GRANTTRACK_ADMIN_*} env vars;
 * change the password after first login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    /** The well-known development admin password; permitted only under the {@code local} profile. */
    static final String DEV_ADMIN_PASSWORD = "Admin@12345";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${granttrack.bootstrap.admin-email:admin@granttrack.local}")
    private String adminEmail;

    @Value("${granttrack.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        String password = resolveAdminPassword();
        roleRepository.findByName(RoleName.ROLE_ADMIN.name()).ifPresent(adminRole -> {
            User admin = User.builder()
                    .name("System Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(password))
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Bootstrapped default admin account: {}", adminEmail);
        });
    }

    /**
     * Resolves the bootstrap admin password. Local dev may fall back to the well-known development
     * password. In any other profile a real password MUST be supplied via
     * {@code granttrack.bootstrap.admin-password} (env {@code GRANTTRACK_BOOTSTRAP_ADMIN_PASSWORD});
     * startup fails fast rather than seeding a guessable administrator.
     */
    private String resolveAdminPassword() {
        boolean localProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");
        boolean isDefaultOrBlank = !StringUtils.hasText(adminPassword)
                || DEV_ADMIN_PASSWORD.equals(adminPassword.trim());
        if (!localProfile && isDefaultOrBlank) {
            throw new IllegalStateException(
                    "No bootstrap admin password configured for a non-local profile. Set "
                            + "GRANTTRACK_BOOTSTRAP_ADMIN_PASSWORD to a strong value before first startup.");
        }
        if (localProfile && !StringUtils.hasText(adminPassword)) {
            return DEV_ADMIN_PASSWORD; // Convenience for local development only.
        }
        return adminPassword;
    }
}

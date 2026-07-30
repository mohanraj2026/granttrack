package com.granttrack.common.audit;

import com.granttrack.common.security.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Supplies the current user id to JPA auditing for {@code created_by}/{@code updated_by}. */
@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<Long> {

    @Override
    @NonNull
    public Optional<Long> getCurrentAuditor() {
        return SecurityUtils.getCurrentUserId();
    }
}

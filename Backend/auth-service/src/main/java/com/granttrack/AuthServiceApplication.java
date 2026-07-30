package com.granttrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Identity &amp; access microservice: authentication, JWT issuance, user/role
 * administration and the audit-log read API. Owns the users, roles, refresh_tokens
 * and audit_logs tables on the shared database.
 *
 * <p>Placed in the {@code com.granttrack} root package so component/entity/repository
 * scanning transparently covers both this service's code and {@code common-lib}.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

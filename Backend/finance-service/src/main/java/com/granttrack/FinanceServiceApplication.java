package com.granttrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Finance microservice: disbursement milestones, evidence review and fund releases against
 * grant awards. Owns the {@code disbursement_milestones} and {@code fund_disbursements} tables;
 * reads {@code grant_awards} / {@code grant_applications} (read-only) from the shared database.
 * Publishes notifications to notification-service via Feign.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}

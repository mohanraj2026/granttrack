package com.granttrack.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for all clients. Discovers services through Eureka and
 * routes each request to the owning microservice (see routes in application.yml).
 * The browser (Angular app) talks only to this gateway.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

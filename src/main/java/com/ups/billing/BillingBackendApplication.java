package com.ups.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

/**
 * Entry point for the Billing Simulation Agent authentication backend.
 *
 * <p>Spring Security's default auto-configuration (login form, filter chain and
 * generated password) is explicitly excluded. We only rely on the
 * {@code BCryptPasswordEncoder} bean for password hashing.
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class BillingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingBackendApplication.class, args);
    }
}

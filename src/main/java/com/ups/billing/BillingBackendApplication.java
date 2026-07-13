package com.ups.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Billing Simulation Agent authentication backend.
 *
 * <p>Security is configured explicitly in {@code SecurityConfig} as a stateless
 * JWT filter chain. Passwords are hashed with {@code BCryptPasswordEncoder}.
 */
@SpringBootApplication
public class BillingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingBackendApplication.class, args);
    }
}

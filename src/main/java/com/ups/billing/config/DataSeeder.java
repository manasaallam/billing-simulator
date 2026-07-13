package com.ups.billing.config;

import com.ups.billing.model.User;
import com.ups.billing.repository.UserRepository;
import com.ups.billing.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Seeds a demo user on startup so the frontend can log in immediately. Runs
 * against whatever datasource is active (H2 today, Supabase/Postgres later) and
 * is idempotent — it never inserts a duplicate.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_EMAIL = "demo@customer.com";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        User demo = new User(
                UUID.randomUUID().toString(),
                AuthService.generateAccountId(),
                "Demo Customer",
                "ACME2026",
                DEMO_EMAIL,
                passwordEncoder.encode("demo1234"),
                "CUSTOMER",
                Instant.now());
        userRepository.save(demo);
    }
}

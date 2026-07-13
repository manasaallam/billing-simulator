package com.ups.billing.store;

import com.ups.billing.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe, in-memory user store keyed by email (lower-cased). No database is
 * used; all data is lost when the application stops.
 */
@Component
public class UserStore {

    private final ConcurrentMap<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder;

    public UserStore(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Seeds a demo user on startup so the frontend can log in immediately.
     */
    @PostConstruct
    void seedDemoUser() {
        String email = "demo@customer.com";
        User demo = new User(
                UUID.randomUUID().toString(),
                generateAccountId(),
                "Demo Customer",
                "ACME2026",
                email,
                passwordEncoder.encode("demo1234"),
                "CUSTOMER",
                Instant.now());
        usersByEmail.put(normalize(email), demo);
    }

    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(normalize(email));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByEmail.get(normalize(email)));
    }

    /**
     * Atomically stores the user only if the email is not already taken.
     *
     * @return {@code true} if stored, {@code false} if the email already exists.
     */
    public boolean saveIfAbsent(User user) {
        return usersByEmail.putIfAbsent(normalize(user.getEmail()), user) == null;
    }

    public static String generateAccountId() {
        return "ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}

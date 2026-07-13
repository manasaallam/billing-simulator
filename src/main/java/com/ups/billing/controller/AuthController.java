package com.ups.billing.controller;

import com.ups.billing.dto.ApiError;
import com.ups.billing.dto.AuthResponse;
import com.ups.billing.dto.LoginRequest;
import com.ups.billing.dto.SignupRequest;
import com.ups.billing.exception.DuplicateEmailException;
import com.ups.billing.exception.InvalidCredentialsException;
import com.ups.billing.model.User;
import com.ups.billing.store.UserStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication endpoints backed by the in-memory {@link UserStore}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserStore userStore;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserStore userStore, BCryptPasswordEncoder passwordEncoder) {
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            ApiError error = new ApiError(
                    "Please correct the highlighted fields.",
                    Map.of("confirmPassword", "Passwords do not match."));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (userStore.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        User user = new User(
                UUID.randomUUID().toString(),
                UserStore.generateAccountId(),
                request.getName().trim(),
                request.getAccessKey(),
                request.getEmail().trim(),
                passwordEncoder.encode(request.getPassword()),
                "CUSTOMER",
                Instant.now());

        if (!userStore.saveIfAbsent(user)) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userStore.findByEmail(request.getEmail())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        return ResponseEntity.ok(AuthResponse.from(user));
    }
}

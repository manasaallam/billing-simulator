package com.ups.billing.service;

import com.ups.billing.dto.AuthResponse;
import com.ups.billing.dto.LoginRequest;
import com.ups.billing.dto.SignupRequest;
import com.ups.billing.exception.DuplicateEmailException;
import com.ups.billing.exception.InvalidCredentialsException;
import com.ups.billing.model.User;
import com.ups.billing.repository.UserRepository;
import com.ups.billing.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Signup / login business logic. Persists users through {@link UserRepository}
 * and issues a JWT session token on success.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        User user = new User(
                UUID.randomUUID().toString(),
                generateAccountId(),
                request.getName().trim(),
                request.getAccessKey(),
                email,
                passwordEncoder.encode(request.getPassword()),
                "CUSTOMER",
                Instant.now());

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Unique-constraint race: another request registered the same email.
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        return AuthResponse.of(user, jwtService.generateToken(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        return AuthResponse.of(user, jwtService.generateToken(user));
    }

    public static String generateAccountId() {
        return "ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

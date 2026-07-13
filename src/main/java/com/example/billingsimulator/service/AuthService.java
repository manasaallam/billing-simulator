package com.example.billingsimulator.service;

import com.example.billingsimulator.dto.AuthResponse;
import com.example.billingsimulator.dto.LoginRequest;
import com.example.billingsimulator.dto.SignupRequest;
import com.example.billingsimulator.exception.DuplicateEmailException;
import com.example.billingsimulator.exception.InvalidCredentialsException;
import com.example.billingsimulator.model.Account;
import com.example.billingsimulator.model.AppUser;
import com.example.billingsimulator.repository.AppUserRepository;
import com.example.billingsimulator.repository.CompanyRepository;
import com.example.billingsimulator.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Signup / login business logic.
 *
 * Signup flow:
 *   1. Validate email uniqueness.
 *   2. Resolve the company from the provided access key (e.g. "DEMO2026").
 *   3. Persist a new app_user row linked to that company.
 *   4. Return a JWT.
 *
 * Login flow: look up user by email, verify BCrypt hash, return JWT.
 */
@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim();

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        // Resolve company by access key — the key ties a user to their company.
        Account company = companyRepository.findByAccessKey(request.getAccessKey())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Access key not recognised. Please check with your company administrator."));

        AppUser user = new AppUser();
        user.setUserId(UUID.randomUUID());
        user.setCompanyId(company.getCompanyId());
        user.setFullName(request.getName().trim());
        user.setEmail(email);
        user.setAuthProviderUid(passwordEncoder.encode(request.getPassword()));
        user.setRole("CUSTOMER");
        user.setCreatedAt(Instant.now());

        try {
            appUserRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another request registered the same email.
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        return AuthResponse.of(user, jwtService.generateToken(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getAuthProviderUid()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        return AuthResponse.of(user, jwtService.generateToken(user));
    }
}

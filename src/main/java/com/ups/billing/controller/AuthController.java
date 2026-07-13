package com.ups.billing.controller;

import com.ups.billing.dto.ApiError;
import com.ups.billing.dto.AuthResponse;
import com.ups.billing.dto.LoginRequest;
import com.ups.billing.dto.SignupRequest;
import com.ups.billing.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication endpoints. Delegates persistence and JWT issuance to
 * {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            ApiError error = new ApiError(
                    "Please correct the highlighted fields.",
                    Map.of("confirmPassword", "Passwords do not match."));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

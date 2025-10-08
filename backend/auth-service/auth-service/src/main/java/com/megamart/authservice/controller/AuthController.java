package com.megamart.authservice.controller;

import com.megamart.authservice.dto.*;
import com.megamart.authservice.service.AuthService;
import com.megamart.authservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new AuthResponse(null, null, null, null, null, e.getMessage())
            );
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.adminLogin(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new AuthResponse(null, null, null, null, null, e.getMessage())
            );
        }
    }

    @PostMapping("/customer/login")
    public ResponseEntity<AuthResponse> customerLogin(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.customerLogin(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new AuthResponse(null, null, null, null, null, e.getMessage())
            );
        }
    }


    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            String token = authHeader.substring(7); // Remove "Bearer "

            if (jwtService.validateToken(token)) {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                Long userId = jwtService.extractUserId(token);

                return ResponseEntity.ok(new TokenValidationResponse(true, email, role, userId));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        } catch (Exception e) {
            log.error("Token validation error", e); // It's good practice to log the error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/sync-user")
    public ResponseEntity<String> syncUser(@RequestBody SyncUserRequestDto request) {
        authService.syncUserFromRegistration(request.getEmail(), request.getHashedPassword(), request.getRole());
        return ResponseEntity.ok("User synced successfully");
    }
}
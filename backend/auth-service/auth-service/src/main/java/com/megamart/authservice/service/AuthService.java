package com.megamart.authservice.service;

import com.megamart.authservice.client.UserServiceClient;
import com.megamart.authservice.dto.AuthResponse;
import com.megamart.authservice.dto.LoginRequest;
import com.megamart.authservice.entity.AuthUser;
import com.megamart.authservice.exception.InvalidCredentialsException;
import com.megamart.authservice.exception.UserNotFoundException;
import com.megamart.authservice.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());
        return authenticateUser(request, null);
    }

    public AuthResponse adminLogin(LoginRequest request) {
        log.info("Admin login attempt for user: {}", request.getEmail());
        return authenticateUser(request, "ADMIN");
    }

    public AuthResponse customerLogin(LoginRequest request) {
        log.info("Customer login attempt for user: {}", request.getEmail());
        return authenticateUser(request, "USER");
    }

    private AuthResponse authenticateUser(LoginRequest request, String expectedRole) {
        AuthUser authUser = authUserRepository.findByEmail(request.getEmail());
        
        if (authUser != null && passwordEncoder.matches(request.getPassword(), authUser.getPassword())) {
            // Role validation for specific login endpoints
            if (expectedRole != null && !authUser.getRole().name().equals(expectedRole)) {
                throw new InvalidCredentialsException("Access denied for this user type");
            }
            
            // Get user details from user-admin service
            com.megamart.authservice.dto.UserInfoDto userInfo = userServiceClient.getUserByEmail(request.getEmail());
            
            String role = authUser.getRole().name(); // Use ADMIN/USER for JWT
            String token = jwtService.generateToken(authUser.getEmail(), role, authUser.getId());
            
            return new AuthResponse(
                token,
                authUser.getId(),
                userInfo != null ? userInfo.getName() : "User",
                authUser.getEmail(),
                authUser.getSpecificRole() != null ? authUser.getSpecificRole() : authUser.getRole().name(),
                "Login successful"
            );
        }
        
        throw new InvalidCredentialsException("Invalid email or password");
    }

    public void syncUserFromRegistration(String email, String hashedPassword, String role) {
        log.info("Syncing user from registration: {} with role: {}", email, role);
        AuthUser authUser = authUserRepository.findByEmail(email);
        if (authUser == null) {
            log.debug("Creating new auth user for: {}", email);
            authUser = new AuthUser();
            authUser.setEmail(email);
        } else {
            log.debug("Updating existing auth user for: {}", email);
        }
        
        authUser.setPassword(hashedPassword);
        authUser.setSpecificRole(role);
        
        // Map user-admin roles to auth roles
        AuthUser.Role authRole;
        switch (role.toLowerCase()) {
            case "super_admin":
            case "admin":
            case "product_manager":
            case "customer_manager":
            case "order_manager":
                authRole = AuthUser.Role.ADMIN;
                break;
            case "customer":
            default:
                authRole = AuthUser.Role.USER;
                break;
        }
        
        authUser.setRole(authRole);
        authUserRepository.save(authUser);
        log.info("Successfully synced user: {} with auth role: {}", email, authRole);
    }
}
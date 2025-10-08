package com.megamart.authservice.service;

import com.megamart.authservice.client.UserServiceClient;
import com.megamart.authservice.dto.*;
import com.megamart.authservice.entity.AuthUser;
import com.megamart.authservice.exception.InvalidCredentialsException;
import com.megamart.authservice.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private AuthUser testUser;
    private LoginRequest loginRequest;
    private UserInfoDto userInfo;

    @BeforeEach
    void setUp() {
        testUser = new AuthUser();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(AuthUser.Role.USER);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");
        
        userInfo = new UserInfoDto();
        userInfo.setId(1L);
        userInfo.setName("Test User");
        userInfo.setEmail("test@example.com");
        userInfo.setPassword("customer");
        userInfo.setRole("customer");
    }

    @Test
    void login_Success() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(userInfo);
        when(jwtService.generateToken("test@example.com", "USER", 1L)).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void login_InvalidCredentials() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_UserNotFound() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(null);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void adminLogin_Success() {
        testUser.setRole(AuthUser.Role.ADMIN);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(userInfo);
        when(jwtService.generateToken("test@example.com", "ADMIN", 1L)).thenReturn("jwt-token");

        AuthResponse response = authService.adminLogin(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void adminLogin_AccessDenied() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () -> authService.adminLogin(loginRequest));
    }

    @Test
    void customerLogin_Success() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(userServiceClient.getUserByEmail("test@example.com")).thenReturn(userInfo);
        when(jwtService.generateToken("test@example.com", "USER", 1L)).thenReturn("jwt-token");

        AuthResponse response = authService.customerLogin(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void customerLogin_AccessDenied() {
        testUser.setRole(AuthUser.Role.ADMIN);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () -> authService.customerLogin(loginRequest));
    }

    @Test
    void syncUserFromRegistration_NewUser() {
        when(authUserRepository.findByEmail("new@example.com")).thenReturn(null);

        authService.syncUserFromRegistration("new@example.com", "hashedPass", "customer");

        verify(authUserRepository).save(argThat(user -> 
            user.getEmail().equals("new@example.com") &&
            user.getPassword().equals("hashedPass") &&
            user.getRole() == AuthUser.Role.USER &&
            user.getSpecificRole().equals("customer")
        ));
    }

    @Test
    void syncUserFromRegistration_ExistingUser() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(testUser);

        authService.syncUserFromRegistration("test@example.com", "newHashedPass", "admin");

        verify(authUserRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getPassword().equals("newHashedPass") &&
            user.getRole() == AuthUser.Role.ADMIN &&
            user.getSpecificRole().equals("admin")
        ));
    }

    @Test
    void syncUserFromRegistration_RoleMapping() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(null);

        authService.syncUserFromRegistration("test@example.com", "hashedPass", "product_manager");

        verify(authUserRepository).save(argThat(user -> 
            user.getRole() == AuthUser.Role.ADMIN &&
            user.getSpecificRole().equals("product_manager")
        ));
    }
}
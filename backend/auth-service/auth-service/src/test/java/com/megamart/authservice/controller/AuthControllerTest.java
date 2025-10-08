package com.megamart.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.authservice.dto.*;
import com.megamart.authservice.service.AuthService;
import com.megamart.authservice.service.JwtService;
import com.megamart.authservice.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private SyncUserRequestDto syncRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword("password");
        
        authResponse = new AuthResponse();
        authResponse.setToken("token");
        authResponse.setId(1L);
        authResponse.setName("User");
        authResponse.setEmail("user@test.com");
        authResponse.setRole("USER");
        authResponse.setMessage("Login successful");
        
        syncRequest = new SyncUserRequestDto();
        syncRequest.setEmail("user@test.com");
        syncRequest.setHashedPassword("hashedpassword");
        syncRequest.setRole("customer");
    }

    @Test
    void login_Success() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void adminLogin_Success() throws Exception {
        AuthResponse adminResponse = new AuthResponse();
        adminResponse.setToken("token");
        adminResponse.setId(1L);
        adminResponse.setName("Admin");
        adminResponse.setEmail("admin@test.com");
        adminResponse.setRole("ADMIN");
        adminResponse.setMessage("Login successful");
        when(authService.adminLogin(any(LoginRequest.class))).thenReturn(adminResponse);

        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminLogin_AccessDenied() throws Exception {
        when(authService.adminLogin(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Access denied for this user type"));

        mockMvc.perform(post("/api/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access denied for this user type"));
    }

    @Test
    void customerLogin_Success() throws Exception {
        AuthResponse customerResponse = new AuthResponse();
        customerResponse.setToken("token");
        customerResponse.setId(1L);
        customerResponse.setName("Customer");
        customerResponse.setEmail("customer@test.com");
        customerResponse.setRole("USER");
        customerResponse.setMessage("Login successful");
        when(authService.customerLogin(any(LoginRequest.class))).thenReturn(customerResponse);

        mockMvc.perform(post("/api/auth/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void customerLogin_AccessDenied() throws Exception {
        when(authService.customerLogin(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Access denied for this user type"));

        mockMvc.perform(post("/api/auth/customer/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access denied for this user type"));
    }

    @Test
    void validateToken_ValidToken() throws Exception {
        when(jwtService.validateToken("validtoken")).thenReturn(true);
        when(jwtService.extractEmail("validtoken")).thenReturn("user@test.com");
        when(jwtService.extractRole("validtoken")).thenReturn("ROLE_USER");
        when(jwtService.extractUserId("validtoken")).thenReturn(1L);

        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer validtoken")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void validateToken_InvalidToken() throws Exception {
        when(jwtService.validateToken("invalidtoken")).thenReturn(false);

        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer invalidtoken")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_MissingAuthHeader() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void validateToken_InvalidAuthHeaderFormat() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "InvalidFormat token")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_TokenException() throws Exception {
        when(jwtService.validateToken("malformedtoken"))
                .thenThrow(new RuntimeException("Malformed token"));

        mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer malformedtoken")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void syncUser_Success() throws Exception {
        doNothing().when(authService).syncUserFromRegistration(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/sync-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(syncRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("User synced successfully"));

        verify(authService).syncUserFromRegistration("user@test.com", "hashedpassword", "customer");
    }
}
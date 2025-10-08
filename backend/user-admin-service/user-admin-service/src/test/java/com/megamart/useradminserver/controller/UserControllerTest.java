package com.megamart.useradminserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.useradminserver.client.AuthServiceClient;
import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.cloud.config.client.ConfigClientAutoConfiguration.class
})
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.config.import="
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    
    @MockBean
    private AuthServiceClient authServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserRegistrationDto registrationDto;
    private UserUpdateDto updateDto;
    private PasswordResetDto resetDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");
        testUser.setPhone("1234567890");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testUser.setGender(User.Gender.male);
        testUser.setRole(User.Role.customer);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("johndoe");
        registrationDto.setEmail("john@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setConfirmPassword("password123");

        updateDto = new UserUpdateDto();
        updateDto.setName("John Updated");
        updateDto.setPhone("9876543210");

        resetDto = new PasswordResetDto();
        resetDto.setEmail("john@example.com");
        resetDto.setOldPassword("oldpass");
        resetDto.setNewPassword("newpass123");
    }

    @Test
    void getAllUsers_ShouldReturnUserList() throws Exception {
        List<User> users = Arrays.asList(testUser);
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("johndoe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"));

        verify(userService).getAllUsers();
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).getUserById(1L);
    }

    @Test
    void getUserProfile_ShouldReturnUserProfile() throws Exception {
        when(userService.getUserProfile(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).getUserProfile(1L);
    }

    @Test
    void getUserByEmail_ShouldReturnUser() throws Exception {
        when(userService.findByUsername("john@example.com")).thenReturn(testUser);

        mockMvc.perform(get("/api/users/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService).findByUsername("john@example.com");
    }

    @Test
    void registerUser_ShouldCreateUser() throws Exception {
        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(testUser);
        doNothing().when(userService).syncWithAuthService(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
        verify(userService).syncWithAuthService(anyString(), anyString(), eq("USER"));
    }

    @Test
    void updateUser_ShouldUpdateUser() throws Exception {
        when(userService.updateUser(eq(1L), any(UserUpdateDto.class))).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).updateUser(eq(1L), any(UserUpdateDto.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).deleteUser(1L);
    }

    @Test
    void resetPassword_ShouldResetPassword() throws Exception {
        doNothing().when(userService).resetPassword(any(PasswordResetDto.class));

        mockMvc.perform(post("/api/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        verify(userService).resetPassword(any(PasswordResetDto.class));
    }
}
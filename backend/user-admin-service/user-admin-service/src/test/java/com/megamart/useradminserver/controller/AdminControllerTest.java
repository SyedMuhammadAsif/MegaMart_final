package com.megamart.useradminserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.useradminserver.client.AuthServiceClient;
import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.Admin;
import com.megamart.useradminserver.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=optional:configserver:",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;
    
    @MockBean
    private AuthServiceClient authServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private Admin testAdmin;
    private AdminCreateDto createDto;
    private AdminProfileUpdateDto updateDto;
    private AdminPasswordChangeDto passwordDto;

    @BeforeEach
    void setUp() {
        testAdmin = new Admin();
        testAdmin.setId(1L);
        testAdmin.setName("Admin User");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setRole(Admin.AdminRole.admin);
        testAdmin.setPermissions(Arrays.asList(Admin.AdminPermission.manage_products));
        testAdmin.setCreatedAt(LocalDateTime.now());
        testAdmin.setUpdatedAt(LocalDateTime.now());

        createDto = new AdminCreateDto();
        createDto.setName("New Admin");
        createDto.setEmail("newadmin@example.com");
        createDto.setPassword("password123");
        createDto.setRole("admin");

        updateDto = new AdminProfileUpdateDto();
        updateDto.setName("Updated Admin");

        passwordDto = new AdminPasswordChangeDto();
        passwordDto.setCurrentPassword("oldpass");
        passwordDto.setNewPassword("newpass123");
    }

    @Test
    void createAdmin_ShouldCreateAdmin() throws Exception {
        when(adminService.createAdmin(any(AdminCreateDto.class))).thenReturn(testAdmin);

        mockMvc.perform(post("/api/users/admin/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin User"))
                .andExpect(jsonPath("$.email").value("admin@example.com"));

        verify(adminService).createAdmin(any(AdminCreateDto.class));
    }

    @Test
    void getAdminById_ShouldReturnAdmin() throws Exception {
        when(adminService.findById(1L)).thenReturn(testAdmin);

        mockMvc.perform(get("/api/users/admin/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Admin User"));

        verify(adminService).findById(1L);
    }

    @Test
    void updateAdminProfile_ShouldUpdateAdmin() throws Exception {
        when(adminService.updateProfile(eq(1L), any(AdminProfileUpdateDto.class))).thenReturn(testAdmin);

        mockMvc.perform(put("/api/users/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin User"));

        verify(adminService).updateProfile(eq(1L), any(AdminProfileUpdateDto.class));
    }

    @Test
    void changeAdminPassword_ShouldChangePassword() throws Exception {
        doNothing().when(adminService).changePassword(eq(1L), any(AdminPasswordChangeDto.class));

        mockMvc.perform(put("/api/users/admin/1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(adminService).changePassword(eq(1L), any(AdminPasswordChangeDto.class));
    }

    @Test
    void uploadProfilePhoto_ShouldUploadPhoto() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "test image".getBytes());
        when(adminService.uploadProfilePhoto(eq(1L), any())).thenReturn("http://example.com/photo.jpg");

        mockMvc.perform(multipart("/api/users/admin/1/upload-photo")
                .file(photo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile photo uploaded successfully"))
                .andExpect(jsonPath("$.photoUrl").value("http://example.com/photo.jpg"));

        verify(adminService).uploadProfilePhoto(eq(1L), any());
    }

    @Test
    void uploadImage_ShouldUploadImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
        when(adminService.uploadImage(any())).thenReturn("http://example.com/image.jpg");

        mockMvc.perform(multipart("/api/users/upload-image")
                .file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image uploaded successfully"))
                .andExpect(jsonPath("$.imageUrl").value("http://example.com/image.jpg"));

        verify(adminService).uploadImage(any());
    }

    @Test
    void getAdminByEmail_ShouldReturnAdmin() throws Exception {
        when(adminService.findByEmail("admin@example.com")).thenReturn(testAdmin);

        mockMvc.perform(get("/api/users/admin/email/admin@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));

        verify(adminService).findByEmail("admin@example.com");
    }
}
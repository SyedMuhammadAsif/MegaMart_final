package com.megamart.useradminserver.service;

import com.megamart.useradminserver.client.AuthServiceClient;
import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.Admin;
import com.megamart.useradminserver.exception.DuplicateResourceException;
import com.megamart.useradminserver.exception.FileUploadException;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AdminServiceImpl adminService;

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
        testAdmin.setPassword("hashedPassword");
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
        updateDto.setEmail("updated@example.com");

        passwordDto = new AdminPasswordChangeDto();
        passwordDto.setCurrentPassword("oldpass");
        passwordDto.setNewPassword("newpass123");
    }

    @Test
    void findByEmail_ShouldReturnAdmin_WhenAdminExists() {
        when(adminRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testAdmin));

        Admin result = adminService.findByEmail("admin@example.com");

        assertEquals(testAdmin, result);
        verify(adminRepository).findByEmail("admin@example.com");
    }

    @Test
    void findByEmail_ShouldThrowException_WhenAdminNotFound() {
        when(adminRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.findByEmail("admin@example.com"));
        verify(adminRepository).findByEmail("admin@example.com");
    }

    @Test
    void findById_ShouldReturnAdmin_WhenAdminExists() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));

        Admin result = adminService.findById(1L);

        assertEquals(testAdmin, result);
        verify(adminRepository).findById(1L);
    }

    @Test
    void findById_ShouldThrowException_WhenAdminNotFound() {
        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.findById(1L));
        verify(adminRepository).findById(1L);
    }

    @Test
    void updateLastLogin_ShouldUpdateLastLogin_WhenAdminExists() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(adminRepository.save(any(Admin.class))).thenReturn(testAdmin);

        adminService.updateLastLogin(1L);

        assertNotNull(testAdmin.getLastLogin());
        verify(adminRepository).findById(1L);
        verify(adminRepository).save(testAdmin);
    }

    @Test
    void createAdmin_ShouldCreateAdmin_WhenValidData() {
        when(adminRepository.existsByEmail("newadmin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(adminRepository.save(any(Admin.class))).thenReturn(testAdmin);
        when(authServiceClient.syncUser(any(SyncUserRequestDto.class))).thenReturn("success");

        Admin result = adminService.createAdmin(createDto);

        assertEquals(testAdmin, result);
        verify(adminRepository).existsByEmail("newadmin@example.com");
        verify(passwordEncoder).encode("password123");
        verify(adminRepository).save(any(Admin.class));
        verify(authServiceClient).syncUser(any(SyncUserRequestDto.class));
    }

    @Test
    void createAdmin_ShouldThrowException_WhenEmailExists() {
        when(adminRepository.existsByEmail("newadmin@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> adminService.createAdmin(createDto));
        verify(adminRepository).existsByEmail("newadmin@example.com");
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void getAllAdmins_ShouldReturnAllAdmins() {
        List<Admin> admins = Arrays.asList(testAdmin);
        when(adminRepository.findAll()).thenReturn(admins);

        List<Admin> result = adminService.getAllAdmins();

        assertEquals(1, result.size());
        assertEquals(testAdmin, result.get(0));
        verify(adminRepository).findAll();
    }

    @Test
    void updateProfile_ShouldUpdateProfile_WhenValidData() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(adminRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(adminRepository.save(any(Admin.class))).thenReturn(testAdmin);

        Admin result = adminService.updateProfile(1L, updateDto);

        assertEquals("Updated Admin", result.getName());
        assertEquals("updated@example.com", result.getEmail());
        verify(adminRepository).findById(1L);
        verify(adminRepository).save(testAdmin);
    }

    @Test
    void updateProfile_ShouldThrowException_WhenEmailAlreadyExists() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(adminRepository.existsByEmail("updated@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> adminService.updateProfile(1L, updateDto));
        verify(adminRepository).findById(1L);
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void changePassword_ShouldChangePassword_WhenValidData() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("oldpass", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("newHashedPassword");
        when(adminRepository.save(any(Admin.class))).thenReturn(testAdmin);
        when(authServiceClient.syncUser(any(SyncUserRequestDto.class))).thenReturn("success");

        Admin result = adminService.changePassword(1L, passwordDto);

        assertEquals(testAdmin, result);
        verify(adminRepository).findById(1L);
        verify(passwordEncoder).matches("oldpass", "hashedPassword");
        verify(passwordEncoder).encode("newpass123");
        verify(adminRepository).save(testAdmin);
        verify(authServiceClient).syncUser(any(SyncUserRequestDto.class));
    }

    @Test
    void changePassword_ShouldThrowException_WhenCurrentPasswordIncorrect() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("oldpass", "hashedPassword")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> adminService.changePassword(1L, passwordDto));
        verify(passwordEncoder).matches("oldpass", "hashedPassword");
        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void uploadProfilePhoto_ShouldThrowException_WhenUploadFails() throws Exception {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getBytes()).thenThrow(new RuntimeException("IO Error"));

        assertThrows(FileUploadException.class, () -> adminService.uploadProfilePhoto(1L, multipartFile));
        verify(adminRepository).findById(1L);
    }

    @Test
    void uploadImage_ShouldThrowException_WhenUploadFails() throws Exception {
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getInputStream()).thenThrow(new IOException("IO Error"));

        assertThrows(FileUploadException.class, () -> adminService.uploadImage(multipartFile));
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenPasswordMatches() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        boolean result = adminService.checkPassword(1L, "password");

        assertTrue(result);
        verify(adminRepository).findById(1L);
        verify(passwordEncoder).matches("password", "hashedPassword");
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenPasswordDoesNotMatch() {
        when(adminRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        boolean result = adminService.checkPassword(1L, "wrongpassword");

        assertFalse(result);
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
    }
}
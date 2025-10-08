package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.Admin;
import com.megamart.useradminserver.exception.DuplicateResourceException;
import com.megamart.useradminserver.exception.FileUploadException;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.repository.AdminRepository;
import com.megamart.useradminserver.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;

    @Override
    public Admin findByEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with email: " + email));
    }

    @Override
    public Admin findById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    }

    @Override
    public void updateLastLogin(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);
    }

    @Override
    public Admin createAdmin(AdminCreateDto adminCreateDto) {
        if (adminRepository.existsByEmail(adminCreateDto.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Admin admin = new Admin();
        admin.setName(adminCreateDto.getName());
        admin.setEmail(adminCreateDto.getEmail());
        String hashedPassword = passwordEncoder.encode(adminCreateDto.getPassword());
        admin.setPassword(hashedPassword);
        Admin.AdminRole adminRole = Admin.AdminRole.valueOf(adminCreateDto.getRole());
        admin.setRole(adminRole);
        admin.setPermissions(getPermissionsForRole(adminRole));
        
        Admin savedAdmin = adminRepository.save(admin);
        
        // Sync with auth service
        syncWithAuthService(admin.getEmail(), hashedPassword, admin.getRole().name());
        
        return savedAdmin;
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public Admin updateProfile(Long adminId, AdminProfileUpdateDto updateDto) {
        Admin admin = findById(adminId);
        
        if (updateDto.getName() != null && !updateDto.getName().trim().isEmpty()) {
            admin.setName(updateDto.getName());
        }
        
        if (updateDto.getEmail() != null && !updateDto.getEmail().trim().isEmpty()) {
            if (!admin.getEmail().equals(updateDto.getEmail()) && 
                adminRepository.existsByEmail(updateDto.getEmail())) {
                throw new DuplicateResourceException("Email already exists");
            }
            admin.setEmail(updateDto.getEmail());
        }
        
        if (updateDto.getPhotoUrl() != null) {
            admin.setPhotoUrl(updateDto.getPhotoUrl());
        }
        
        return adminRepository.save(admin);
    }

    @Override
    public Admin changePassword(Long adminId, AdminPasswordChangeDto passwordDto) {
        Admin admin = findById(adminId);
        
        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), admin.getPassword())) {
            throw new ResourceNotFoundException("Current password is incorrect");
        }
        
        String hashedPassword = passwordEncoder.encode(passwordDto.getNewPassword());
        admin.setPassword(hashedPassword);
        
        Admin savedAdmin = adminRepository.save(admin);
        
        // Sync new password with auth service
        syncWithAuthService(admin.getEmail(), hashedPassword, admin.getRole().name());
        
        return savedAdmin;
    }

    private void syncWithAuthService(String email, String hashedPassword, String role) {
        try {
            SyncUserRequestDto request = new SyncUserRequestDto(email, hashedPassword, role);
            authServiceClient.syncUser(request);
        } catch (Exception e) {
            log.error("Failed to sync admin {} with auth service: {}", email, e.getMessage());
        }
    }

    @Override
    public String uploadProfilePhoto(Long adminId, MultipartFile photo) {
        Admin admin = findById(adminId);
        
        try {
            String uploadDir = "uploads/admin-photos/";
            Path uploadPath = Paths.get(uploadDir);
            
            Files.createDirectories(uploadPath);
            
            String fileName = UUID.randomUUID().toString() + "_" + photo.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, photo.getBytes());
            
            String photoUrl = "/uploads/admin-photos/" + fileName;
            admin.setPhotoUrl(photoUrl);
            adminRepository.save(admin);
            
            return photoUrl;
        } catch (Exception e) {
            throw new FileUploadException("Failed to upload photo: " + e.getMessage());
        }
    }

    @Override
    public String uploadImage(MultipartFile image) {
        try {
            String uploadDir = "uploads/images/";
            Path uploadPath = Paths.get(uploadDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(image.getInputStream(), filePath);
            
            return "/uploads/images/" + fileName;
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    public boolean checkPassword(Long adminId, String password) {
        Admin admin = findById(adminId);
        return passwordEncoder.matches(password, admin.getPassword());
    }

    private List<Admin.AdminPermission> getPermissionsForRole(Admin.AdminRole role) {
        switch (role) {
            case super_admin:
                return Arrays.asList(Admin.AdminPermission.values());
            case admin:
                return Arrays.asList(Admin.AdminPermission.manage_products, Admin.AdminPermission.manage_orders, Admin.AdminPermission.manage_customers, Admin.AdminPermission.view_analytics);
            case product_manager:
                return Arrays.asList(Admin.AdminPermission.manage_products, Admin.AdminPermission.view_analytics);
            case customer_manager:
                return Arrays.asList(Admin.AdminPermission.manage_customers, Admin.AdminPermission.view_analytics);
            case order_manager:
                return Arrays.asList(Admin.AdminPermission.manage_orders, Admin.AdminPermission.view_analytics);
            default:
                return Arrays.asList(Admin.AdminPermission.view_analytics);
        }
    }
}
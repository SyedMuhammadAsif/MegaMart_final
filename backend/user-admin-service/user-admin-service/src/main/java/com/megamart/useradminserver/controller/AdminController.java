package com.megamart.useradminserver.controller;

import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.Admin;
import com.megamart.useradminserver.service.AdminService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "APIs for admin management")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/admin/create")
    @Operation(summary = "Create admin user", description = "Create new admin user")
    public ResponseEntity<AdminResponseDto> createAdmin(@Valid @RequestBody AdminCreateDto adminCreateDto) {
        Admin admin = adminService.createAdmin(adminCreateDto);
        return ResponseEntity.ok(AdminResponseDto.fromEntity(admin));
    }

    @GetMapping("/admin/{id}")
    @Operation(summary = "Get admin by ID", description = "Retrieve admin details by ID")
    public ResponseEntity<AdminResponseDto> getAdminById(@PathVariable Long id) {
        Admin admin = adminService.findById(id);
        return ResponseEntity.ok(AdminResponseDto.fromEntity(admin));
    }

    @PutMapping("/admin/{id}")
    @Operation(summary = "Update admin profile", description = "Update admin profile information")
    public ResponseEntity<AdminResponseDto> updateAdminProfile(@PathVariable Long id, @Valid @RequestBody AdminProfileUpdateDto updateDto) {
        Admin updatedAdmin = adminService.updateProfile(id, updateDto);
        return ResponseEntity.ok(AdminResponseDto.fromEntity(updatedAdmin));
    }

    @PutMapping("/admin/{id}/password")
    @Operation(summary = "Change admin password", description = "Change admin password")
    public ResponseEntity<MessageDto> changeAdminPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordChangeDto passwordDto) {
        adminService.changePassword(id, passwordDto);
        return ResponseEntity.ok(new MessageDto("Password changed successfully"));
    }

    @PostMapping("/admin/{id}/upload-photo")
    @Operation(summary = "Upload admin profile photo", description = "Upload profile photo for admin")
    public ResponseEntity<Map<String, Object>> uploadProfilePhoto(@PathVariable Long id, @RequestParam("photo") MultipartFile photo) {
        String photoUrl = adminService.uploadProfilePhoto(id, photo);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile photo uploaded successfully");
        response.put("photoUrl", photoUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-image")
    @Operation(summary = "Upload image", description = "Upload any image file")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("image") MultipartFile image) {
        String imageUrl = adminService.uploadImage(image);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Image uploaded successfully");
        response.put("imageUrl", imageUrl);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/email/{email}")
    @Operation(summary = "Get admin by email", description = "Retrieve admin details by email")
    public ResponseEntity<AdminResponseDto> getAdminByEmail(@PathVariable String email) {
        Admin admin = adminService.findByEmail(email);
        return ResponseEntity.ok(AdminResponseDto.fromEntity(admin));
    }
}
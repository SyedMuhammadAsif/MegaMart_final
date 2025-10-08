package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.Admin;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminService {
    Admin findByEmail(String email);
    Admin findById(Long id);
    void updateLastLogin(Long adminId);
    Admin createAdmin(AdminCreateDto adminCreateDto);
    List<Admin> getAllAdmins();
    Admin updateProfile(Long adminId, AdminProfileUpdateDto updateDto);
    Admin changePassword(Long adminId, AdminPasswordChangeDto passwordDto);
    String uploadProfilePhoto(Long adminId, MultipartFile photo);
    String uploadImage(MultipartFile image);
    boolean checkPassword(Long adminId, String password);
}
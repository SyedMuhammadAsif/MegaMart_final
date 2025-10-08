package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User getUserProfile(Long userId);
    User registerUser(UserRegistrationDto registrationDto);
    AuthResponseDto loginUser(LoginDto loginDto);
    User getUserByUserId(Long userId);
    User findByUsername(String username);
    User createUser(UserRegistrationDto registrationDto);
    User updateUser(Long id, UserUpdateDto updateDto);
    void deleteUser(Long id);
    void resetPassword(PasswordResetDto resetDto);
    void syncWithAuthService(String email, String hashedPassword, String role);
}
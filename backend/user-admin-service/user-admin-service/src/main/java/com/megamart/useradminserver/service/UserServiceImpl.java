package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.exception.DuplicateResourceException;
import com.megamart.useradminserver.repository.UserRepository;
import com.megamart.useradminserver.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    
    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthServiceClient authServiceClient;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + id));
    }

    @Override
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + userId));
    }

    @Override
    public User registerUser(UserRegistrationDto registrationDto) {
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new DuplicateResourceException("Passwords do not match");
        }

        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + registrationDto.getUsername());
        }

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + registrationDto.getEmail());
        }

        User user = new User();
        user.setName(null);
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        String hashedPassword = passwordEncoder.encode(registrationDto.getPassword());
        user.setPassword(hashedPassword);
        user.setPhone(null);
        user.setDateOfBirth(null);
        user.setGender(null);

        if (registrationDto.getEmail().contains("admin") || registrationDto.getUsername().contains("admin")) {
            user.setRole(User.Role.admin);
        } else {
            user.setRole(User.Role.customer);
        }

        User savedUser = userRepository.save(user);
        

        syncWithAuthService(user.getEmail(), hashedPassword, user.getRole().name());
        
        return savedUser;
    }

    @Override
    public AuthResponseDto loginUser(LoginDto loginDto) {
        throw new UnsupportedOperationException("Login is now handled by auth-service");
    }

    public void syncWithAuthService(String email, String hashedPassword, String role) {
        try {
            SyncUserRequestDto request = new SyncUserRequestDto(email, hashedPassword, role);
            authServiceClient.syncUser(request);
        } catch (Exception e) {
            log.error("Failed to sync {} with auth service: {}", email, e.getMessage());
        }
    }

    @Override
    public User getUserByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + userId));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User createUser(UserRegistrationDto registrationDto) {
        return registerUser(registrationDto);
    }

    @Override
    public User updateUser(Long id, UserUpdateDto updateDto) {
        User user = getUserById(id);
        
        if (updateDto.getName() != null) user.setName(updateDto.getName());
        if (updateDto.getPhone() != null) user.setPhone(updateDto.getPhone());
        if (updateDto.getDateOfBirth() != null) user.setDateOfBirth(updateDto.getDateOfBirth());
        if (updateDto.getGender() != null) user.setGender(updateDto.getGender());

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void resetPassword(PasswordResetDto resetDto) {
        User user = userRepository.findByEmail(resetDto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + resetDto.getEmail()));

        // Validate old password
        if (!passwordEncoder.matches(resetDto.getOldPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Invalid old password");
        }

        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userRepository.save(user);
    }
}
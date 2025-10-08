package com.megamart.useradminserver.controller;

import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user registration and profile management")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponseDto> userDtos = users.stream().map(UserResponseDto::fromEntity).toList();
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserResponseDto> getUserProfile(@PathVariable Long userId) {
        User user = userService.getUserProfile(userId);
        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponseDto> getUserByEmail(@PathVariable String email) {
        User user = userService.findByUsername(email);
        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account with customer or admin role")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        User user = userService.registerUser(registrationDto);
        // Sync with auth service
        userService.syncWithAuthService(user.getEmail(), user.getPassword(), "USER");
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDto.fromEntity(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDto updateDto) {
        User user = userService.updateUser(id, updateDto);
        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageDto> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageDto("User deleted successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageDto> resetPassword(@Valid @RequestBody PasswordResetDto resetDto) {
        userService.resetPassword(resetDto);
        return ResponseEntity.ok(new MessageDto("Password reset successfully"));
    }
}
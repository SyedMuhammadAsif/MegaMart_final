package com.megamart.useradminserver.service;

import com.megamart.useradminserver.client.AuthServiceClient;
import com.megamart.useradminserver.dto.*;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.exception.DuplicateResourceException;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private UserServiceImpl userService;

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
        testUser.setPassword("hashedPassword");
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
        updateDto.setDateOfBirth(LocalDate.of(1985, 5, 15));
        updateDto.setGender(User.Gender.male);

        resetDto = new PasswordResetDto();
        resetDto.setEmail("john@example.com");
        resetDto.setOldPassword("oldpass");
        resetDto.setNewPassword("newpass123");
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertEquals(testUser, result);
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserProfile_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserProfile(1L);

        assertEquals(testUser, result);
        verify(userRepository).findById(1L);
    }

    @Test
    void registerUser_ShouldCreateUser_WhenValidData() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authServiceClient.syncUser(any(SyncUserRequestDto.class))).thenReturn("success");

        User result = userService.registerUser(registrationDto);

        assertEquals(testUser, result);
        verify(userRepository).existsByUsername("johndoe");
        verify(userRepository).existsByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(authServiceClient).syncUser(any(SyncUserRequestDto.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenPasswordsDontMatch() {
        registrationDto.setConfirmPassword("differentPassword");

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(registrationDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenUsernameExists() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(registrationDto));
        verify(userRepository).existsByUsername("johndoe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(registrationDto));
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldSetAdminRole_WhenEmailContainsAdmin() {
        registrationDto.setEmail("admin@example.com");
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(User.Role.admin, user.getRole());
            return user;
        });

        userService.registerUser(registrationDto);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        User result = userService.findByUsername("johndoe");

        assertEquals(testUser, result);
        verify(userRepository).findByUsername("johndoe");
    }

    @Test
    void findByUsername_ShouldReturnNull_WhenUserNotFound() {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());

        User result = userService.findByUsername("johndoe");

        assertNull(result);
        verify(userRepository).findByUsername("johndoe");
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUser(1L, updateDto);

        assertEquals("John Updated", result.getName());
        assertEquals("9876543210", result.getPhone());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(1L, updateDto));
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(1L);
    }

    @Test
    void resetPassword_ShouldResetPassword_WhenValidData() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpass", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.resetPassword(resetDto);

        verify(userRepository).findByEmail("john@example.com");
        verify(passwordEncoder).matches("oldpass", "hashedPassword");
        verify(passwordEncoder).encode("newpass123");
        verify(userRepository).save(testUser);
    }

    @Test
    void resetPassword_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.resetPassword(resetDto));
        verify(userRepository).findByEmail("john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrowException_WhenOldPasswordInvalid() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldpass", "hashedPassword")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.resetPassword(resetDto));
        verify(passwordEncoder).matches("oldpass", "hashedPassword");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void syncWithAuthService_ShouldHandleException_WhenServiceFails() {
        when(authServiceClient.syncUser(any(SyncUserRequestDto.class))).thenThrow(new RuntimeException("Service unavailable"));

        assertDoesNotThrow(() -> userService.syncWithAuthService("test@example.com", "password", "USER"));
        verify(authServiceClient).syncUser(any(SyncUserRequestDto.class));
    }

    @Test
    void loginUser_ShouldThrowUnsupportedOperationException() {
        LoginDto loginDto = new LoginDto();
        
        assertThrows(UnsupportedOperationException.class, () -> userService.loginUser(loginDto));
    }
}
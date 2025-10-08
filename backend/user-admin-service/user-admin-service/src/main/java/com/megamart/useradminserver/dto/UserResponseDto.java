package com.megamart.useradminserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.megamart.useradminserver.entity.User;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private java.time.LocalDate dateOfBirth;
    private String gender;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponseDto fromEntity(User user) {
        return new UserResponseDto(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getDateOfBirth(),
            user.getGender() != null ? user.getGender().name() : null,
            user.getRole() != null ? user.getRole().name() : null,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
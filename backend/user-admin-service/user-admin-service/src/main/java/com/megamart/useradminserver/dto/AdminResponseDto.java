package com.megamart.useradminserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.megamart.useradminserver.entity.Admin;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponseDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private java.util.List<String> permissions;
    private String photoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    public static AdminResponseDto fromEntity(Admin admin) {
        return new AdminResponseDto(
            admin.getId(),
            admin.getName(),
            admin.getEmail(),
            admin.getRole() != null ? admin.getRole().name() : null,
            admin.getPermissions() != null ? admin.getPermissions().stream().map(Enum::name).toList() : null,
            admin.getPhotoUrl(),
            admin.getCreatedAt(),
            admin.getUpdatedAt(),
            admin.getLastLogin()
        );
    }
}
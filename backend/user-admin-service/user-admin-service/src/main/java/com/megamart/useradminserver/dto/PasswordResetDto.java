package com.megamart.useradminserver.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class PasswordResetDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}
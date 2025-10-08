package com.megamart.useradminserver.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginDto {
    @NotBlank(message = "Username or email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
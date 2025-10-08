package com.megamart.useradminserver.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class LoginDto {
    @NotBlank(message = "Username or email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
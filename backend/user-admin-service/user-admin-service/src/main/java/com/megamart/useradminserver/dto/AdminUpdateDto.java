package com.megamart.useradminserver.dto;

import com.megamart.useradminserver.entity.User;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class AdminUpdateDto {
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private User.Role role;
}
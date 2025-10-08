package com.megamart.useradminserver.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import com.megamart.useradminserver.entity.User;

import java.time.LocalDate;

@Data
public class UserUpdateDto {
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Pattern(regexp = "^\\d{10,20}$", message = "Phone number should be valid")
    private String phone;

    private LocalDate dateOfBirth;

    private User.Gender gender;
}
package com.megamart.useradminserver.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class AddressDto {
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be 3-100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.]+$", message = "Full name should contain only letters, spaces and dots")
    private String fullName;

    @NotBlank(message = "Address line 1 is required")
    @Size(min = 5, max = 200, message = "Address line 1 must be 5-200 characters")
    private String addressLine1;

    @Size(max = 200, message = "Address line 2 must not exceed 200 characters")
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be 2-100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "City should contain only letters and spaces")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State must be 2-100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "State should contain only letters and spaces")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Postal code must be exactly 6 digits")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 100, message = "Country must be 2-100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Country should contain only letters and spaces")
    private String country;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    private Boolean isDefault = false;
}
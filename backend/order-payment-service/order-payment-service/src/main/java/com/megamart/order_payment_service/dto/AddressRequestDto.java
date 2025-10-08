package com.megamart.order_payment_service.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequestDto {
    @Pattern(regexp = "^.{1,100}$", message = "Full name is required")
    private String fullName;
    
    @Pattern(regexp = "^.{1,200}$", message = "Address line 1 is required")
    private String addressLine1;
    
    private String addressLine2;
    private String city;
    private String state;
    
    @Pattern(regexp = "^\\d{5,10}$", message = "Postal code must be 5-10 digits")
    private String postalCode;
    
    private String country;
    
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;
    
    private Boolean isDefault;
}
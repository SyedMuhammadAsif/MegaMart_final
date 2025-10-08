package com.megamart.cartwishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponseDto {
    private boolean valid;
    private String email;
    private String role;
    private Long userId;
}
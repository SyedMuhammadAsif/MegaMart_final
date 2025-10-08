package com.megamart.useradminserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncUserRequestDto {
    private String email;
    private String hashedPassword;
    private String role;
}
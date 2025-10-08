package com.megamart.useradminserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.megamart.useradminserver.entity.UserAddress;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressResponseDto {
    private Long id;
    private String fullName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserAddressResponseDto fromEntity(UserAddress address) {
        return new UserAddressResponseDto(
            address.getId(),
            address.getFullName(),
            address.getAddressLine1(),
            address.getAddressLine2(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountry(),
            address.getPhone(),
            address.getIsDefault(),
            address.getCreatedAt(),
            address.getUpdatedAt()
        );
    }
}
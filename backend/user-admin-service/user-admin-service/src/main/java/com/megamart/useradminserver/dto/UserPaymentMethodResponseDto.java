package com.megamart.useradminserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.megamart.useradminserver.entity.UserPaymentMethod;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPaymentMethodResponseDto {
    private Long id;
    private String type;
    private String cardNumber;
    private String cardholderName;
    private String expiryMonth;
    private String expiryYear;
    private String upiId;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserPaymentMethodResponseDto fromEntity(UserPaymentMethod method) {
        return new UserPaymentMethodResponseDto(
            method.getId(),
            method.getType() != null ? method.getType().name() : null,
            method.getCardNumber(),
            method.getCardholderName(),
            method.getExpiryMonth(),
            method.getExpiryYear(),
            method.getUpiId(),
            method.getIsDefault(),
            method.getCreatedAt(),
            method.getUpdatedAt()
        );
    }
}
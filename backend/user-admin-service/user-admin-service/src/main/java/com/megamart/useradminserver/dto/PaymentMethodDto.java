package com.megamart.useradminserver.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import com.megamart.useradminserver.entity.UserPaymentMethod;
import com.megamart.useradminserver.validation.ValidPaymentMethod;

@Data
@ValidPaymentMethod
public class PaymentMethodDto {
    @NotNull(message = "Payment type is required")
    private UserPaymentMethod.PaymentType type;

    @Pattern(regexp = "^[0-9\\s]{13,19}$", message = "Card number should be 13-19 digits")
    private String cardNumber;

    @Size(min = 2, max = 100, message = "Cardholder name must be 2-100 characters")
    private String cardholderName;

    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month should be valid (01-12)")
    private String expiryMonth;

    @Pattern(regexp = "^\\d{4}$", message = "Expiry year should be valid (YYYY)")
    private String expiryYear;

    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$", message = "UPI ID format is invalid")
    private String upiId;

    private Boolean isDefault = false;
}
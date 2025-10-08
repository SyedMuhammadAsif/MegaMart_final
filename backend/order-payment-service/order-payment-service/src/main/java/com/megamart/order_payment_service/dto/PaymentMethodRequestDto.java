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
public class PaymentMethodRequestDto {
    @Pattern(regexp = "^(CARD|UPI|COD)$", message = "Payment type must be CARD, UPI, or COD")
    private String type;
    
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be exactly 16 digits")
    private String cardNumber;
    private String cardholderName;
    
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be 01-12")
    private String expiryMonth;
    
    @Pattern(regexp = "^20[2-9]\\d$", message = "Expiry year must be 2020-2099")
    private String expiryYear;
    
    @Pattern(regexp = "^\\d{3}$", message = "CVV must be exactly 3 digits")
    private String cvv;
    
    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$", message = "Invalid UPI ID format")
    private String upiId;
    
    private Boolean isDefault;
}
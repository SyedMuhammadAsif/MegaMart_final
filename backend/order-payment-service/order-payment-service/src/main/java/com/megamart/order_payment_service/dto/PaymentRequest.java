package com.megamart.order_payment_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    // Payment method - either use existing (provide ID) or create new (provide details)
    private Long paymentMethodId;
    private PaymentMethodRequestDto newPaymentMethod;
} 
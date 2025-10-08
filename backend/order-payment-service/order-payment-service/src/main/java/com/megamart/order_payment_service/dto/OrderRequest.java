package com.megamart.order_payment_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal total;
    
    @NotNull(message = "Payment type is required")
    @Pattern(regexp = "^(CARD|UPI|COD)$", message = "Payment type must be CARD, UPI, or COD")
    private String paymentType;
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequestDto> items;
    
    // Address - either use existing (provide ID) or create new (provide details)
    private Long addressId;
    private AddressRequestDto newAddress;
    
    // Payment method - either use existing (provide ID) or create new (provide details)
    private Long paymentMethodId;
    private PaymentMethodRequestDto newPaymentMethod;
    

} 
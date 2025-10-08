package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    
    private Long id;
    private Long userId;
    private BigDecimal total;
    private String paymentType;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime orderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private AddressDto shippingAddress;
    private List<OrderItemDto> orderItems;
    private PaymentDto payment;
    
    // Customer information
    private String customerName;
    private String customerEmail;
    private String customerPhone;
} 
package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private Long id;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String transactionId;
    private PaymentMethodDto paymentMethod;
}
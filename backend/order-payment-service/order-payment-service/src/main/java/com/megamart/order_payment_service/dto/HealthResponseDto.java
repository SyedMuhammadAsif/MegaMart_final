package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponseDto {
    private String service;
    private String status;
    private String port;
    private Long timestamp;
}
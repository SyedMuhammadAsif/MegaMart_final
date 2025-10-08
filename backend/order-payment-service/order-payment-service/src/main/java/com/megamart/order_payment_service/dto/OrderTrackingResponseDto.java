package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.megamart.order_payment_service.entity.OrderTracking;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponseDto {
    private Long orderId;
    private List<OrderTracking> trackingHistory;
}
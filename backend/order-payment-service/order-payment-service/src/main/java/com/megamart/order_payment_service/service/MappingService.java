package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.entity.Order;
import com.megamart.order_payment_service.entity.OrderItem;
import com.megamart.order_payment_service.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class MappingService {
    
    public OrderResponse mapToOrderResponse(Order order, AddressDto shippingAddress, PaymentMethodDto paymentMethod) {
        // Use stored shipping address if available, otherwise use provided address
        AddressDto finalShippingAddress = shippingAddress;
        if (order.getShippingFullName() != null) {
            finalShippingAddress = AddressDto.builder()
                    .id(order.getShippingAddressId())
                    .fullName(order.getShippingFullName())
                    .addressLine1(order.getShippingAddressLine1())
                    .addressLine2(order.getShippingAddressLine2())
                    .city(order.getShippingCity())
                    .state(order.getShippingState())
                    .postalCode(order.getShippingPostalCode())
                    .country(order.getShippingCountry())
                    .phone(order.getShippingPhone())
                    .build();
        }
        
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .total(order.getTotal())
                .paymentType(order.getPaymentType().name())
                .orderStatus(order.getOrderStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .orderDate(order.getOrderDate())
                .shippingAddress(finalShippingAddress)
                .orderItems(mapOrderItems(order.getOrderItems()))
                .payment(mapPayment(order.getPayment(), paymentMethod))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    public PaymentDto mapToPaymentResponse(Payment payment, PaymentMethodDto paymentMethod) {
        return PaymentDto.builder()
                .id(payment.getId())
                .paymentStatus(payment.getPaymentStatus().name())
                .paymentDate(payment.getPaymentDate())
                .transactionId(payment.getTransactionId())
                .paymentMethod(paymentMethod)
                .build();
    }
    
    private List<OrderItemDto> mapOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null) {
            return Collections.emptyList();
        }
        
        return orderItems.stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();
    }
    
    private PaymentDto mapPayment(Payment payment, PaymentMethodDto paymentMethod) {
        if (payment == null) {
            return null;
        }
        
        return PaymentDto.builder()
                .id(payment.getId())
                .paymentStatus(payment.getPaymentStatus().name())
                .paymentDate(payment.getPaymentDate())
                .transactionId(payment.getTransactionId())
                .paymentMethod(paymentMethod)
                .build();
    }
} 
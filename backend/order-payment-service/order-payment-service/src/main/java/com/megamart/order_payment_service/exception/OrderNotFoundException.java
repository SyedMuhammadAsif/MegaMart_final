package com.megamart.order_payment_service.exception;

public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }
    
    public OrderNotFoundException(String message) {
        super(message);
    }
} 
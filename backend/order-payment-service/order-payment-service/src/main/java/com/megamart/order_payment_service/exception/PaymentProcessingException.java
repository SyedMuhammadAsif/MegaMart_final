package com.megamart.order_payment_service.exception;

public class PaymentProcessingException extends RuntimeException {
    
    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PaymentProcessingException(Long orderId, String reason) {
        super(String.format("Payment processing failed for order %d: %s", orderId, reason));
    }
} 
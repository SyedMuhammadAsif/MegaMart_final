package com.megamart.order_payment_service.exception;

public class PaymentMethodNotFoundException extends RuntimeException {
    
    public PaymentMethodNotFoundException(Integer userId, Long paymentMethodId) {
        super("Payment method not found for user: " + userId + " and paymentMethodId: " + paymentMethodId);
    }
    
    public PaymentMethodNotFoundException(String message) {
        super(message);
    }
} 
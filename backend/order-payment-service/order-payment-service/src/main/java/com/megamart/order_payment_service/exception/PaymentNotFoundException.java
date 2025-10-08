package com.megamart.order_payment_service.exception;

public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(Long orderId) {
        super("Payment not found for order id: " + orderId);
    }
    
    public PaymentNotFoundException(String transactionId, boolean isTransactionId) {
        super("Payment not found for transaction id: " + transactionId+" "+isTransactionId);
    }
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
} 
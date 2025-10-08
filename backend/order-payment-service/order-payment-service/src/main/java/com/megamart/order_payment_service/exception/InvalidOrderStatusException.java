package com.megamart.order_payment_service.exception;

public class InvalidOrderStatusException extends RuntimeException {
    
    public InvalidOrderStatusException(String message) {
        super(message);
    }
    
    public InvalidOrderStatusException(Long orderId, String currentStatus, String attemptedOperation) {
        super(String.format("Cannot %s order %d. Current status: %s", attemptedOperation, orderId, currentStatus));
    }
} 
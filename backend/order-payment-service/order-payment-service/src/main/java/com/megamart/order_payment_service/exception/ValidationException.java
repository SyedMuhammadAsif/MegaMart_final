package com.megamart.order_payment_service.exception;

public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String field, String message) {
        super(field + ": " + message);
    }
} 
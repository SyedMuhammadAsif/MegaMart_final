package com.megamart.order_payment_service.exception;

public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String field, String reason) {
        super(String.format("Invalid %s: %s", field, reason));
    }
} 
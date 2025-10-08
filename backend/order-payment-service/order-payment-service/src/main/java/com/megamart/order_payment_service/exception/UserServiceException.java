package com.megamart.order_payment_service.exception;

public class UserServiceException extends RuntimeException {
    
    public UserServiceException(String message) {
        super(message);
    }
    
    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UserServiceException(String operation, Integer userId) {
        super(String.format("User service error during %s for user %s", operation, userId));
    }
} 
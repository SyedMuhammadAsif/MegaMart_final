package com.megamart.order_payment_service.exception;

public class AddressNotFoundException extends RuntimeException {
    
    public AddressNotFoundException(Integer userId, Long addressId) {
        super("Address not found for user: " + userId + " and addressId: " + addressId);
    }
    
    public AddressNotFoundException(String message) {
        super(message);
    }
} 
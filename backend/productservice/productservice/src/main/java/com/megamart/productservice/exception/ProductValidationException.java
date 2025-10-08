package com.megamart.productservice.exception;

public class ProductValidationException extends RuntimeException {
    public ProductValidationException(String message) {
        super(message);
    }
    
    public ProductValidationException(String field, String value) {

        super("Invalid " + field + ": " + value);
    }
}
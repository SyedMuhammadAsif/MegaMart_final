package com.megamart.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SUCCESS = "success";
    private static final String ERROR = "error";
    private static final String TIMESTAMP = "timestamp";

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(SUCCESS, false);
        response.put(ERROR, ex.getMessage());
        response.put(TIMESTAMP, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCategoryNotFound(CategoryNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(SUCCESS, false);
        response.put(ERROR, ex.getMessage());
        response.put(TIMESTAMP, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ProductValidationException.class)
    public ResponseEntity<Map<String, Object>> handleProductValidation(ProductValidationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(SUCCESS, false);
        response.put(ERROR, ex.getMessage());
        response.put(TIMESTAMP, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(SUCCESS, false);
        response.put(ERROR, ex.getMessage());
        response.put(TIMESTAMP, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(SUCCESS, false);
        response.put(ERROR, "Internal server error");
        response.put("details", ex.getMessage());
        response.put(TIMESTAMP, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
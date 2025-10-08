package com.megamart.order_payment_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String BAD_REQUEST = "Bad Request";

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFoundException(OrderNotFoundException ex) {
        log.error("Order not found: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.NOT_FOUND.value());
        error.put(ERROR, "Not Found");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.error("Payment not found: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.NOT_FOUND.value());
        error.put(ERROR, "Not Found");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAddressNotFoundException(AddressNotFoundException ex) {
        log.error("Address not found: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.NOT_FOUND.value());
        error.put(ERROR, "Address Not Found");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PaymentMethodNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentMethodNotFoundException(PaymentMethodNotFoundException ex) {
        log.error("Payment method not found: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.NOT_FOUND.value());
        error.put(ERROR, "Payment Method Not Found");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, "Validation Error");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, Object>> handleServiceException(ServiceException ex) {
        log.error("Service error: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(ERROR, "Service Error");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrderStatusException(InvalidOrderStatusException ex) {
        log.error("Invalid order status operation: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, BAD_REQUEST);
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentProcessingException(PaymentProcessingException ex) {
        log.error("Payment processing failed: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.PAYMENT_REQUIRED.value());
        error.put(ERROR, "Payment Processing Failed");
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequestException(InvalidRequestException ex) {
        log.error("Invalid request: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, BAD_REQUEST);
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<Map<String, Object>> handleUserServiceException(UserServiceException ex) {
        log.error("User service error: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.SERVICE_UNAVAILABLE.value());
        error.put(ERROR, "Service Unavailable");
        error.put(MESSAGE, "User service is currently unavailable: " + ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation exception occurred: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, "Validation Failed");
        error.put(MESSAGE, "Invalid request data");
        error.put("validationErrors", validationErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.BAD_REQUEST.value());
        error.put(ERROR, BAD_REQUEST);
        error.put(MESSAGE, ex.getMessage());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put(TIMESTAMP, LocalDateTime.now());
        error.put(STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(ERROR, "Internal Server Error");
        error.put(MESSAGE, "An unexpected error occurred");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
} 
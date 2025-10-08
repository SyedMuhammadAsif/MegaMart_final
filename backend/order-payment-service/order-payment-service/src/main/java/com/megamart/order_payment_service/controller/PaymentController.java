package com.megamart.order_payment_service.controller;

import com.megamart.order_payment_service.dto.PaymentRequest;
import com.megamart.order_payment_service.dto.PaymentDto;
import com.megamart.order_payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private static final String REFUND_TIME = "refundTime";
    private static final String MESSAGE = "message";
    private static final String REFUND_METHOD = "refundMethod";
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentDto> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());
        PaymentDto paymentResponse = paymentService.processPayment(request);
        return ResponseEntity.ok(paymentResponse);
    }
    
    @PostMapping("/refund/{orderId}")
    public ResponseEntity<Map<String, Object>> refundPayment(@PathVariable Long orderId) {
        log.info("Processing refund for order: {}", orderId);
        
        PaymentDto payment = paymentService.getPaymentByOrderId(orderId);
        
        // Check if payment can be refunded
        if (!"COMPLETED".equals(payment.getPaymentStatus())) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cannot refund payment");
            error.put("reason", "Payment status is: " + payment.getPaymentStatus());
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> refundResult = processRefundByPaymentType(payment);
        
        return ResponseEntity.ok(refundResult);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentDto paymentResponse = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(paymentResponse);
    }
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentDto> getPaymentByTransactionId(@PathVariable String transactionId) {
        PaymentDto paymentResponse = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(paymentResponse);
    }
    
    private Map<String, Object> processRefundByPaymentType(PaymentDto payment) {
        Map<String, Object> refundResult = new HashMap<>();
        String paymentType = payment.getPaymentMethod().getType();
        
        refundResult.put("orderId", payment.getId());
        refundResult.put("originalTransactionId", payment.getTransactionId());
        refundResult.put("paymentType", paymentType);
        refundResult.put("refundAmount", "Amount will be refunded");
        
        switch (paymentType) {
            case "CARD":
                refundResult.put(REFUND_METHOD, "Credit Card Refund");
                refundResult.put(REFUND_TIME, "3-5 business days");
                refundResult.put("refundTo", "Original card ending in " + 
                    payment.getPaymentMethod().getCardNumber().substring(
                        payment.getPaymentMethod().getCardNumber().length() - 4));
                refundResult.put(MESSAGE, "Refund initiated to your card. It will appear in 3-5 business days.");
                break;
                
            case "UPI":
                refundResult.put(REFUND_METHOD, "UPI Refund");
                refundResult.put(REFUND_TIME, "Instant to 2 hours");
                refundResult.put("refundTo", payment.getPaymentMethod().getUpiId());
                refundResult.put(MESSAGE, "Refund initiated to your UPI account. It should appear within 2 hours.");
                break;
                
            case "COD":
                refundResult.put(REFUND_METHOD, "No refund needed");
                refundResult.put(REFUND_TIME, "N/A");
                refundResult.put(MESSAGE, "No refund needed for Cash on Delivery orders.");
                break;
                
            default:
                refundResult.put(MESSAGE, "Refund method not supported for this payment type.");
        }
        
        return refundResult;
    }
} 
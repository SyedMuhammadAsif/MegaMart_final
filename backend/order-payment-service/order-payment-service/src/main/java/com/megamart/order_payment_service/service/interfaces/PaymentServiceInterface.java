package com.megamart.order_payment_service.service.interfaces;

import com.megamart.order_payment_service.dto.*;

public interface PaymentServiceInterface {
    
    PaymentDto processPayment(PaymentRequest request);
    
    PaymentDto getPaymentByOrderId(Long orderId);
    
    PaymentDto getPaymentByTransactionId(String transactionId);
} 
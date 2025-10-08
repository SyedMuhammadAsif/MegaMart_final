package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.dto.PaymentRequest;
import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.entity.Order;
import com.megamart.order_payment_service.entity.Payment;
import com.megamart.order_payment_service.exception.InvalidOrderStatusException;
import com.megamart.order_payment_service.exception.InvalidRequestException;
import com.megamart.order_payment_service.exception.OrderNotFoundException;
import com.megamart.order_payment_service.exception.PaymentNotFoundException;
import com.megamart.order_payment_service.exception.PaymentProcessingException;
import com.megamart.order_payment_service.repository.OrderRepository;
import com.megamart.order_payment_service.repository.PaymentRepository;
import com.megamart.order_payment_service.service.interfaces.PaymentServiceInterface;
import com.megamart.order_payment_service.service.interfaces.UserDataServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService implements PaymentServiceInterface {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserDataServiceInterface userDataService;
    private final MappingService mappingService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public PaymentDto processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());
        
        checkIfPaymentRequestIsValid(request);
        
        Order order = findOrderById(request.getOrderId());
        checkIfCanProcessPayment(order);
        
        PaymentMethodDto paymentMethod = getOrCreatePaymentMethod(request, order.getUserId());
        
        Payment payment = createOrUpdatePayment(order, paymentMethod.getId());
        boolean paymentSuccess = processPaymentWithGateway(paymentMethod);
        updatePaymentStatus(payment, order, paymentSuccess);
        
        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);
        
        return mappingService.mapToPaymentResponse(savedPayment, paymentMethod);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));
        
        PaymentMethodDto paymentMethod = getPaymentMethodDetails(payment);
        return mappingService.mapToPaymentResponse(payment, paymentMethod);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException(transactionId, true));
        
        PaymentMethodDto paymentMethod = getPaymentMethodDetails(payment);
        return mappingService.mapToPaymentResponse(payment, paymentMethod);
    }
    
    private void checkIfPaymentRequestIsValid(PaymentRequest request) {
        if (request.getPaymentMethodId() == null && request.getNewPaymentMethod() == null) {
            throw new InvalidRequestException("paymentMethod", "Either paymentMethodId or newPaymentMethod must be provided");
        }
    }
    
    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    private void checkIfCanProcessPayment(Order order) {
        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Cannot process payment for cancelled order");
        }
        if (order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("Cannot process payment for delivered order");
        }
    }
    
    private PaymentMethodDto getOrCreatePaymentMethod(PaymentRequest request, Long userId) {
        if (request.getPaymentMethodId() != null) {
            return getPaymentMethodDetails(userId, request.getPaymentMethodId());
        } else {
            // Hash CVV before storing
            if (request.getNewPaymentMethod() != null && request.getNewPaymentMethod().getCvv() != null) {
                String hashedCvv = hashCvv(request.getNewPaymentMethod().getCvv());
                request.getNewPaymentMethod().setCvv(hashedCvv);
            }
            return userDataService.createPaymentMethod(userId, request.getNewPaymentMethod());
        }
    }
    
    private String hashCvv(String cvv) {
        return passwordEncoder.encode(cvv);
    }
    

    
    private Payment createOrUpdatePayment(Order order, Long paymentMethodId) {
        Payment payment = order.getPayment();
        
        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .userId(order.getUserId())
                    .amount(order.getTotal())
                    .paymentMethodId(paymentMethodId)
                    .paymentStatus(Payment.PaymentStatus.PROCESSING)
                    .paymentDate(LocalDateTime.now())
                    .transactionId(generateTransactionId())
                    .build();
        } else {
            payment.setAmount(order.getTotal());
            payment.setPaymentMethodId(paymentMethodId);
            payment.setPaymentStatus(Payment.PaymentStatus.PROCESSING);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setTransactionId(generateTransactionId());
        }
        
        return payment;
    }
    
    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private boolean processPaymentWithGateway(PaymentMethodDto paymentMethod) {
        try {
            Thread.sleep(1000);
            return switch (paymentMethod.getType()) {
                case "COD" -> true;
                case "UPI" -> Math.random() > 0.1;
                case "CARD" -> Math.random() > 0.05;
                default -> true;
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private void updatePaymentStatus(Payment payment, Order order, boolean paymentSuccess) {
        if (paymentSuccess) {
            payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setOrderStatus(Order.OrderStatus.CONFIRMED);
        } else {
            payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            throw new PaymentProcessingException(order.getId(), "Payment gateway declined");
        }
    }
    
    private PaymentMethodDto getPaymentMethodDetails(Long userId, Long paymentMethodId) {
        try {
            return userDataService.getPaymentMethod(userId, paymentMethodId);
        } catch (Exception e) {
            log.error("Error getting payment method: {}", e.getMessage());
            throw new PaymentProcessingException("Failed to get payment method: " + e.getMessage());
        }
    }
    
    private PaymentMethodDto getPaymentMethodDetails(Payment payment) {
        if (payment.getPaymentMethodId() != null) {
            return getPaymentMethodDetails(payment.getUserId(), payment.getPaymentMethodId());
        }
        return null;
    }
} 
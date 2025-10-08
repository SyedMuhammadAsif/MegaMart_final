package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.dto.PaymentDto;
import com.megamart.order_payment_service.dto.PaymentMethodDto;
import com.megamart.order_payment_service.dto.PaymentMethodRequestDto;
import com.megamart.order_payment_service.dto.PaymentRequest;
import com.megamart.order_payment_service.entity.Order;
import com.megamart.order_payment_service.entity.Payment;
import com.megamart.order_payment_service.exception.OrderNotFoundException;
import com.megamart.order_payment_service.exception.PaymentNotFoundException;
import com.megamart.order_payment_service.exception.PaymentProcessingException;
import com.megamart.order_payment_service.repository.OrderRepository;
import com.megamart.order_payment_service.repository.PaymentRepository;
import com.megamart.order_payment_service.service.interfaces.UserDataServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserDataServiceInterface userDataService;

    @Mock
    private MappingService mappingService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Order order;
    private Payment payment;
    private PaymentDto paymentDto;
    private PaymentMethodDto paymentMethodDto;

    @BeforeEach
    void setUp() {
        paymentRequest = PaymentRequest.builder()
                .orderId(1L)
                .paymentMethodId(1L)
                .build();

        order = Order.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(999.99))
                .paymentType(Order.PaymentType.CARD)
                .orderStatus(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();

        payment = Payment.builder()
                .id(1L)
                .userId(1L)
                .amount(BigDecimal.valueOf(999.99))
                .paymentStatus(Payment.PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .transactionId("TXN123456")
                .build();

        paymentDto = PaymentDto.builder()
                .id(1L)
                .paymentStatus("COMPLETED")
                .paymentDate(LocalDateTime.now())
                .transactionId("TXN123456")
                .build();

        paymentMethodDto = PaymentMethodDto.builder()
                .id(1L)
                .type("CARD")
                .cardNumber("****1234")
                .cardholderName("John Doe")
                .build();
    }

    @Test
    void processPayment_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToPaymentResponse(any(Payment.class), any(PaymentMethodDto.class)))
                .thenReturn(paymentDto);

        PaymentDto result = paymentService.processPayment(paymentRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("COMPLETED", result.getPaymentStatus());
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void processPayment_WithNewPaymentMethod() {
        paymentRequest.setPaymentMethodId(null);
        paymentRequest.setNewPaymentMethod(PaymentMethodRequestDto.builder()
                .type("UPI")
                .upiId("user@paytm")
                .build());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.createPaymentMethod(eq(1L), any(PaymentMethodRequestDto.class)))
                .thenReturn(paymentMethodDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToPaymentResponse(any(Payment.class), any(PaymentMethodDto.class)))
                .thenReturn(paymentDto);

        PaymentDto result = paymentService.processPayment(paymentRequest);

        assertNotNull(result);
        verify(userDataService).createPaymentMethod(eq(1L), any(PaymentMethodRequestDto.class));
    }

    @Test
    void processPayment_OrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> paymentService.processPayment(paymentRequest));
    }

    @Test
    void processPayment_CODOrder() {
        order.setPaymentType(Order.PaymentType.COD);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
    }

    @Test
    void processPayment_AlreadyPaid() {
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
    }

    @Test
    void getPaymentByOrderId_Success() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(userDataService.getPaymentMethod(1L, null)).thenReturn(paymentMethodDto);
        when(mappingService.mapToPaymentResponse(any(Payment.class), any(PaymentMethodDto.class)))
                .thenReturn(paymentDto);

        PaymentDto result = paymentService.getPaymentByOrderId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(paymentRepository).findByOrderId(1L);
    }

    @Test
    void getPaymentByOrderId_NotFound() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByOrderId(1L));
    }

    @Test
    void processPayment_CardPayment_Success() {
        order.setPaymentType(Order.PaymentType.CARD);
        paymentMethodDto.setType("CARD");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToPaymentResponse(any(Payment.class), any(PaymentMethodDto.class)))
                .thenReturn(paymentDto);

        PaymentDto result = paymentService.processPayment(paymentRequest);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getPaymentStatus());
    }

    @Test
    void processPayment_UPIPayment_Success() {
        order.setPaymentType(Order.PaymentType.UPI);
        paymentMethodDto.setType("UPI");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToPaymentResponse(any(Payment.class), any(PaymentMethodDto.class)))
                .thenReturn(paymentDto);

        PaymentDto result = paymentService.processPayment(paymentRequest);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getPaymentStatus());
    }

    @Test
    void processPayment_PaymentMethodNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(null);

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
    }

    @Test
    void processPayment_InvalidPaymentMethodType() {
        paymentMethodDto.setType("INVALID");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
    }

    @Test
    void processPayment_UPIPayment_Success2() {
        order.setPaymentType(Order.PaymentType.UPI);
        paymentMethodDto.setType("UPI");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToPaymentResponse(any(Payment.class), any(PaymentMethodDto.class)))
                .thenReturn(paymentDto);

        PaymentDto result = paymentService.processPayment(paymentRequest);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getPaymentStatus());
    }

    @Test
    void processPayment_MissingBothPaymentMethods() {
        paymentRequest.setPaymentMethodId(null);
        paymentRequest.setNewPaymentMethod(null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
    }

    @Test
    void processPayment_CancelledOrder() {
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
    }
}
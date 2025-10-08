package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.entity.Order;
import com.megamart.order_payment_service.entity.OrderItem;
import com.megamart.order_payment_service.entity.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MappingServiceTest {

    @InjectMocks
    private MappingService mappingService;

    private Order order;
    private AddressDto addressDto;
    private PaymentMethodDto paymentMethodDto;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(999.99))
                .paymentType(Order.PaymentType.CARD)
                .orderStatus(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.COMPLETED)
                .orderDate(LocalDateTime.now())
                .shippingAddressId(1L)
                .build();

        addressDto = AddressDto.builder()
                .id(1L)
                .fullName("John Doe")
                .addressLine1("123 Main St")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .phone("1234567890")
                .build();

        paymentMethodDto = PaymentMethodDto.builder()
                .id(1L)
                .type("CARD")
                .cardNumber("****1234")
                .cardholderName("John Doe")
                .build();

        payment = Payment.builder()
                .id(1L)
                .paymentStatus(Payment.PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .transactionId("TXN123")
                .build();
    }

    @Test
    void mapToOrderResponse_WithProvidedAddress() {
        OrderResponse result = mappingService.mapToOrderResponse(order, addressDto, paymentMethodDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("PENDING", result.getOrderStatus());
        assertEquals("COMPLETED", result.getPaymentStatus());
        assertEquals(addressDto, result.getShippingAddress());
    }

    @Test
    void mapToOrderResponse_WithStoredShippingInfo() {
        order.setShippingFullName("Jane Doe");
        order.setShippingAddressLine1("456 Oak St");
        order.setShippingCity("Boston");
        order.setShippingState("MA");
        order.setShippingPostalCode("02101");
        order.setShippingCountry("USA");
        order.setShippingPhone("9876543210");

        OrderResponse result = mappingService.mapToOrderResponse(order, addressDto, paymentMethodDto);

        assertNotNull(result);
        assertEquals("Jane Doe", result.getShippingAddress().getFullName());
        assertEquals("456 Oak St", result.getShippingAddress().getAddressLine1());
        assertEquals("Boston", result.getShippingAddress().getCity());
    }

    @Test
    void mapToOrderResponse_WithOrderItems() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .lineTotal(BigDecimal.valueOf(999.99))
                .build();
        order.setOrderItems(Arrays.asList(item));

        OrderResponse result = mappingService.mapToOrderResponse(order, addressDto, paymentMethodDto);

        assertNotNull(result.getOrderItems());
        assertEquals(1, result.getOrderItems().size());
        assertEquals(1L, result.getOrderItems().get(0).getProductId());
    }

    @Test
    void mapToOrderResponse_WithPayment() {
        order.setPayment(payment);

        OrderResponse result = mappingService.mapToOrderResponse(order, addressDto, paymentMethodDto);

        assertNotNull(result.getPayment());
        assertEquals("COMPLETED", result.getPayment().getPaymentStatus());
        assertEquals("TXN123", result.getPayment().getTransactionId());
    }

    @Test
    void mapToOrderResponse_WithNullOrderItems() {
        order.setOrderItems(null);

        OrderResponse result = mappingService.mapToOrderResponse(order, addressDto, paymentMethodDto);

        assertNotNull(result.getOrderItems());
        assertTrue(result.getOrderItems().isEmpty());
    }

    @Test
    void mapToOrderResponse_WithNullPayment() {
        order.setPayment(null);

        OrderResponse result = mappingService.mapToOrderResponse(order, addressDto, paymentMethodDto);

        assertNull(result.getPayment());
    }

    @Test
    void mapToPaymentResponse_Success() {
        PaymentDto result = mappingService.mapToPaymentResponse(payment, paymentMethodDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("COMPLETED", result.getPaymentStatus());
        assertEquals("TXN123", result.getTransactionId());
        assertEquals(paymentMethodDto, result.getPaymentMethod());
    }
}
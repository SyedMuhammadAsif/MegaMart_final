package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.client.CartServiceClient;
import com.megamart.order_payment_service.client.ProductServiceClient;
import com.megamart.order_payment_service.client.UserAdminClient;
import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.entity.Order;
import com.megamart.order_payment_service.entity.OrderItem;
import com.megamart.order_payment_service.entity.OrderTracking;
import com.megamart.order_payment_service.entity.Payment;
import com.megamart.order_payment_service.exception.InvalidOrderStatusException;
import com.megamart.order_payment_service.exception.InvalidRequestException;
import com.megamart.order_payment_service.exception.OrderNotFoundException;
import com.megamart.order_payment_service.repository.OrderRepository;
import com.megamart.order_payment_service.repository.OrderTrackingRepository;
import com.megamart.order_payment_service.service.interfaces.UserDataServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderTrackingRepository trackingRepository;

    @Mock
    private UserDataServiceInterface userDataService;

    @Mock
    private MappingService mappingService;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private UserAdminClient userAdminClient;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private Order order;
    private OrderResponse orderResponse;
    private AddressDto addressDto;
    private PaymentMethodDto paymentMethodDto;

    @BeforeEach
    void setUp() {
        orderRequest = OrderRequest.builder()
                .userId(1L)
                .total(BigDecimal.valueOf(999.99))
                .paymentType("CARD")
                .addressId(1L)
                .paymentMethodId(1L)
                .items(Arrays.asList(
                        OrderItemRequestDto.builder()
                                .productId(1L)
                                .quantity(2)
                                .lineTotal(BigDecimal.valueOf(999.99))
                                .build()
                ))
                .build();

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

        orderResponse = OrderResponse.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(999.99))
                .orderStatus("PENDING")
                .paymentStatus("COMPLETED")
                .paymentType("CARD")
                .orderDate(LocalDateTime.now())
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
    }

    @Test
    void createOrder_Success() {
        when(userDataService.getAddress(1L, 1L)).thenReturn(addressDto);
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(orderRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_WithNewAddress() {
        orderRequest.setAddressId(null);
        orderRequest.setNewAddress(AddressRequestDto.builder()
                .fullName("John Doe")
                .addressLine1("123 Main St")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .phone("1234567890")
                .build());

        when(userDataService.createAddress(eq(1L), any(AddressRequestDto.class))).thenReturn(addressDto);
        when(userDataService.getPaymentMethod(1L, 1L)).thenReturn(paymentMethodDto);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(orderRequest);

        assertNotNull(result);
        verify(userDataService).createAddress(eq(1L), any(AddressRequestDto.class));
    }

    @Test
    void createOrder_InvalidRequest_NoAddress() {
        orderRequest.setAddressId(null);
        orderRequest.setNewAddress(null);

        assertThrows(InvalidRequestException.class, () -> orderService.createOrder(orderRequest));
    }

    @Test
    void createOrder_InvalidRequest_NoPaymentMethod() {
        orderRequest.setPaymentMethodId(null);
        orderRequest.setNewPaymentMethod(null);

        assertThrows(InvalidRequestException.class, () -> orderService.createOrder(orderRequest));
    }

    @Test
    void getOrderById_Success() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(userDataService.getAddress(1L, 1L)).thenReturn(addressDto);
        when(userDataService.getPaymentMethod(1L, null)).thenReturn(paymentMethodDto);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);
        when(userAdminClient.getUserById(1L)).thenReturn(Map.of("name", "John Doe", "email", "john@example.com", "phone", "1234567890"));

        OrderResponse result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(orderRepository).findByIdWithDetails(1L);
    }

    @Test
    void getOrderById_NotFound() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void getUserOrders_Success() {
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class))).thenReturn(orderPage);
        when(userDataService.getAddress(1L, 1L)).thenReturn(addressDto);
        when(userDataService.getPaymentMethod(1L, null)).thenReturn(paymentMethodDto);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        Page<OrderResponse> result = orderService.getUserOrders(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class));
    }

    @Test
    void getAllOrders_Success() {
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(order));
        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(orderPage);

        Page<OrderResponse> result = orderService.getAllOrders(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(orderRepository).findAll(any(PageRequest.class));
    }

    @Test
    void updateOrderStatus_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(userDataService.getAddress(1L, 1L)).thenReturn(addressDto);
        when(userDataService.getPaymentMethod(1L, null)).thenReturn(paymentMethodDto);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        OrderResponse result = orderService.updateOrderStatus(1L, "CONFIRMED", 1L, "Order confirmed");

        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
        verify(trackingRepository).save(any(OrderTracking.class));
    }

    @Test
    void updateOrderStatus_InvalidTransition() {
        order.setOrderStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, 
                () -> orderService.updateOrderStatus(1L, "PENDING"));
    }

    @Test
    void cancelOrder_Success() {
        OrderItem orderItem = OrderItem.builder()
                .productId(1L)
                .quantity(2)
                .build();
        order.setOrderItems(Arrays.asList(orderItem));
        
        Payment payment = Payment.builder()
                .paymentStatus(Payment.PaymentStatus.COMPLETED)
                .build();
        order.setPayment(payment);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(userDataService.getAddress(1L, 1L)).thenReturn(addressDto);
        when(userDataService.getPaymentMethod(1L, null)).thenReturn(paymentMethodDto);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        OrderResponse result = orderService.cancelOrder(1L);

        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
        verify(productServiceClient).updateStock(eq(1L), any(Map.class));
    }

    @Test
    void cancelOrder_AlreadyCancelled() {
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void cancelOrder_AlreadyDelivered() {
        order.setOrderStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void createOrderFromCart_Success() {
        CartResponseDto cart = new CartResponseDto();
        cart.setTotalPrice(999.99);
        CartItemDto cartItem = new CartItemDto();
        cartItem.setProductId(1L);
        cartItem.setQuantity(2);
        cartItem.setLineTotal(999.99);
        cart.setItems(Arrays.asList(cartItem));

        ProductDataDto productData = new ProductDataDto();
        productData.setStock(10);
        ProductResponseDto productResponse = new ProductResponseDto();
        productResponse.setSuccess(true);
        productResponse.setData(productData);

        when(cartServiceClient.getCart("1")).thenReturn(cart);
        when(productServiceClient.getProductById(1L)).thenReturn(productResponse);
        when(userDataService.createAddress(eq(1L), any(AddressRequestDto.class))).thenReturn(addressDto);
        when(userDataService.createPaymentMethod(eq(1L), any(PaymentMethodRequestDto.class))).thenReturn(paymentMethodDto);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        AddressRequestDto address = AddressRequestDto.builder()
                .fullName("John Doe")
                .addressLine1("123 Main St")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .phone("1234567890")
                .build();

        PaymentMethodRequestDto paymentMethod = PaymentMethodRequestDto.builder()
                .type("CARD")
                .cardNumber("1234567890123456")
                .cardholderName("John Doe")
                .expiryMonth("12")
                .expiryYear("2025")
                .cvv("123")
                .build();

        OrderResponse result = orderService.createOrderFromCart(1L, address, paymentMethod);

        assertNotNull(result);
        verify(cartServiceClient).getCart("1");
        verify(productServiceClient).getProductById(1L);
        verify(productServiceClient).updateStock(eq(1L), any(Map.class));
        verify(cartServiceClient).clearCart("1");
    }

    @Test
    void createOrderFromCart_EmptyCart() {
        CartResponseDto cart = new CartResponseDto();
        cart.setItems(Arrays.asList());

        when(cartServiceClient.getCart("1")).thenReturn(cart);

        AddressRequestDto address = AddressRequestDto.builder().build();
        PaymentMethodRequestDto paymentMethod = PaymentMethodRequestDto.builder().build();

        assertThrows(InvalidRequestException.class, 
                () -> orderService.createOrderFromCart(1L, address, paymentMethod));
    }

    @Test
    void createOrderFromCart_InsufficientStock() {
        CartResponseDto cart = new CartResponseDto();
        CartItemDto cartItem = new CartItemDto();
        cartItem.setProductId(1L);
        cartItem.setQuantity(10);
        cart.setItems(Arrays.asList(cartItem));

        ProductDataDto productData = new ProductDataDto();
        productData.setStock(5);
        ProductResponseDto productResponse = new ProductResponseDto();
        productResponse.setSuccess(true);
        productResponse.setData(productData);

        when(cartServiceClient.getCart("1")).thenReturn(cart);
        when(productServiceClient.getProductById(1L)).thenReturn(productResponse);

        AddressRequestDto address = AddressRequestDto.builder().build();
        PaymentMethodRequestDto paymentMethod = PaymentMethodRequestDto.builder().build();

        assertThrows(InvalidRequestException.class, 
                () -> orderService.createOrderFromCart(1L, address, paymentMethod));
    }

    @Test
    void getOrderTrackingHistory_Success() {
        List<OrderTracking> trackingList = Arrays.asList(
                OrderTracking.builder()
                        .orderId(1L)
                        .status("PENDING")
                        .description("Order created")
                        .build()
        );

        when(trackingRepository.findByOrderIdOrderByCreatedAtAsc(1L)).thenReturn(trackingList);

        List<OrderTracking> result = orderService.getOrderTrackingHistory(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(trackingRepository).findByOrderIdOrderByCreatedAtAsc(1L);
    }

    @Test
    void deleteOrder_Success() {
        OrderItem orderItem = OrderItem.builder()
                .productId(1L)
                .quantity(2)
                .build();
        order.setOrderItems(Arrays.asList(orderItem));
        order.setOrderStatus(Order.OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(trackingRepository.findByOrderIdOrderByCreatedAtAsc(1L)).thenReturn(Arrays.asList());

        orderService.deleteOrder(1L);

        verify(orderRepository).delete(order);
        verify(productServiceClient).updateStock(eq(1L), any(Map.class));
    }

    @Test
    void createOrder_WithNewPaymentMethod() {
        orderRequest.setPaymentMethodId(null);
        orderRequest.setNewPaymentMethod(PaymentMethodRequestDto.builder()
                .type("UPI")
                .upiId("user@paytm")
                .build());

        when(userDataService.getAddress(1L, 1L)).thenReturn(addressDto);
        when(userDataService.createPaymentMethod(eq(1L), any(PaymentMethodRequestDto.class))).thenReturn(paymentMethodDto);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mappingService.mapToOrderResponse(any(Order.class), any(AddressDto.class), any(PaymentMethodDto.class)))
                .thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(orderRequest);

        assertNotNull(result);
        verify(userDataService).createPaymentMethod(eq(1L), any(PaymentMethodRequestDto.class));
    }

    @Test
    void updateOrderStatus_OrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, 
                () -> orderService.updateOrderStatus(999L, "CONFIRMED"));
    }

    @Test
    void createOrderFromCart_ProductNotFound() {
        CartResponseDto cart = new CartResponseDto();
        CartItemDto cartItem = new CartItemDto();
        cartItem.setProductId(999L);
        cartItem.setQuantity(1);
        cart.setItems(Arrays.asList(cartItem));

        ProductResponseDto productResponse = new ProductResponseDto();
        productResponse.setSuccess(false);

        when(cartServiceClient.getCart("1")).thenReturn(cart);
        when(productServiceClient.getProductById(999L)).thenReturn(productResponse);

        AddressRequestDto address = AddressRequestDto.builder().build();
        PaymentMethodRequestDto paymentMethod = PaymentMethodRequestDto.builder().build();

        assertThrows(InvalidRequestException.class, 
                () -> orderService.createOrderFromCart(1L, address, paymentMethod));
    }
}
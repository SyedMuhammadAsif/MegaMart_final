package com.megamart.order_payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.entity.OrderTracking;
import com.megamart.order_payment_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRequest orderRequest;
    private OrderResponse orderResponse;
    private OrderFromCartRequestDto cartRequest;

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

        orderResponse = OrderResponse.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(999.99))
                .orderStatus("PENDING")
                .paymentStatus("COMPLETED")
                .paymentType("CARD")
                .orderDate(LocalDateTime.now())
                .build();

        cartRequest = new OrderFromCartRequestDto();
        cartRequest.setAddress(AddressRequestDto.builder()
                .fullName("John Doe")
                .addressLine1("123 Main St")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .phone("1234567890")
                .build());
        cartRequest.setPaymentMethod(PaymentMethodRequestDto.builder()
                .type("CARD")
                .cardNumber("1234567890123456")
                .cardholderName("John Doe")
                .expiryMonth("12")
                .expiryYear("2025")
                .cvv("123")
                .build());
    }

    @Test
    @WithMockUser
    void createOrder_Success() throws Exception {
        when(orderService.createOrder(any(OrderRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.total").value(999.99))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"));
    }

    @Test
    @WithMockUser
    void createOrder_InvalidRequest() throws Exception {
        orderRequest.setUserId(null);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createOrderFromCart_Success() throws Exception {
        when(orderService.createOrderFromCart(eq(1L), any(AddressRequestDto.class), any(PaymentMethodRequestDto.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/orders/from-cart/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithMockUser
    void createOrderFromCart_InvalidRequest() throws Exception {
        cartRequest.setAddress(null);

        mockMvc.perform(post("/api/orders/from-cart/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getOrderById_Success() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.total").value(999.99));
    }

    @Test
    @WithMockUser
    void getUserOrders_Success() throws Exception {
        Page<OrderResponse> orderPage = new PageImpl<>(Arrays.asList(orderResponse));
        when(orderService.getUserOrders(eq(1L), any(PageRequest.class))).thenReturn(orderPage);

        mockMvc.perform(get("/api/orders/user/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].userId").value(1L));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getAllOrders_Success() throws Exception {
        Page<OrderResponse> orderPage = new PageImpl<>(Arrays.asList(orderResponse));
        when(orderService.getAllOrders(any(PageRequest.class))).thenReturn(orderPage);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @WithMockUser
    void getAllOrders_Forbidden() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateOrderStatus_Success() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), eq("PROCESSING"), eq(1L), eq("Processing order")))
                .thenReturn(orderResponse);

        mockMvc.perform(put("/api/orders/1/status")
                        .with(csrf())
                        .param("status", "PROCESSING")
                        .param("locationId", "1")
                        .param("notes", "Processing order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    void updateOrderStatus_Forbidden() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .with(csrf())
                        .param("status", "PROCESSING"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void cancelOrder_Success() throws Exception {
        OrderResponse cancelledOrder = OrderResponse.builder()
                .id(1L)
                .userId(1L)
                .orderStatus("CANCELLED")
                .paymentStatus("REFUNDED")
                .build();
        when(orderService.cancelOrder(1L)).thenReturn(cancelledOrder);

        mockMvc.perform(put("/api/orders/1/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteOrder_Success() throws Exception {
        mockMvc.perform(delete("/api/orders/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order deleted successfully"))
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @WithMockUser
    void deleteOrder_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/orders/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getOrderTracking_Success() throws Exception {
        List<OrderTracking> trackingList = Arrays.asList(
                OrderTracking.builder()
                        .orderId(1L)
                        .status("PENDING")
                        .description("Order created")
                        .build()
        );
        when(orderService.getOrderTrackingHistory(1L)).thenReturn(trackingList);

        mockMvc.perform(get("/api/orders/1/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.trackingHistory[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void health_Success() throws Exception {
        mockMvc.perform(get("/api/orders/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("Order-Payment Service"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.port").value("9098"));
    }

    @Test
    @WithMockUser
    void testOrders_Success() throws Exception {
        mockMvc.perform(get("/api/orders/debug/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("Order service is running"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser
    void createOrder_ServiceException() throws Exception {
        when(orderService.createOrder(any(OrderRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void getOrderById_NotFound() throws Exception {
        when(orderService.getOrderById(999L))
                .thenThrow(new RuntimeException("Order not found"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void getUserOrders_EmptyResult() throws Exception {
        Page<OrderResponse> emptyPage = new PageImpl<>(Arrays.asList());
        when(orderService.getUserOrders(eq(1L), any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/orders/user/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateOrderStatus_MissingParameters() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
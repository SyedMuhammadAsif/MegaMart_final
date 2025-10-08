package com.megamart.order_payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.order_payment_service.dto.PaymentDto;
import com.megamart.order_payment_service.dto.PaymentMethodRequestDto;
import com.megamart.order_payment_service.dto.PaymentRequest;
import com.megamart.order_payment_service.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest paymentRequest;
    private PaymentDto paymentDto;

    @BeforeEach
    void setUp() {
        paymentRequest = PaymentRequest.builder()
                .orderId(1L)
                .paymentMethodId(1L)
                .build();

        paymentDto = PaymentDto.builder()
                .id(1L)
                .paymentStatus("COMPLETED")
                .paymentDate(LocalDateTime.now())
                .transactionId("TXN123456")
                .build();
    }

    @Test
    @WithMockUser
    void processPayment_Success() throws Exception {
        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(paymentDto);

        mockMvc.perform(post("/api/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void processPayment_WithNewPaymentMethod() throws Exception {
        PaymentRequest requestWithNewMethod = PaymentRequest.builder()
                .orderId(1L)
                .newPaymentMethod(PaymentMethodRequestDto.builder()
                        .type("UPI")
                        .upiId("user@paytm")
                        .build())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(paymentDto);

        mockMvc.perform(post("/api/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithNewMethod)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser
    void processPayment_InvalidRequest() throws Exception {
        paymentRequest.setOrderId(null);

        mockMvc.perform(post("/api/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getPaymentByOrderId_Success() throws Exception {
        when(paymentService.getPaymentByOrderId(1L)).thenReturn(paymentDto);

        mockMvc.perform(get("/api/payments/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void getPaymentByOrderId_NotFound() throws Exception {
        when(paymentService.getPaymentByOrderId(999L))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/payments/order/999"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void processPayment_ServiceException() throws Exception {
        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Payment processing failed"));

        mockMvc.perform(post("/api/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void processPayment_MissingPaymentMethod() throws Exception {
        PaymentRequest invalidRequest = PaymentRequest.builder()
                .orderId(1L)
                .build();

        mockMvc.perform(post("/api/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
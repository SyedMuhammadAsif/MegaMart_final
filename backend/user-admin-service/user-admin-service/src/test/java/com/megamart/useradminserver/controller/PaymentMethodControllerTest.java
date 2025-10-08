package com.megamart.useradminserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.useradminserver.dto.PaymentMethodDto;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.entity.UserPaymentMethod;
import com.megamart.useradminserver.service.PaymentMethodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentMethodController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentMethodService paymentMethodService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPaymentMethod testPaymentMethod;
    private PaymentMethodDto paymentMethodDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");

        testPaymentMethod = new UserPaymentMethod();
        testPaymentMethod.setId(1L);
        testPaymentMethod.setUser(testUser);
        testPaymentMethod.setType(UserPaymentMethod.PaymentType.card);
        testPaymentMethod.setCardNumber("**** **** **** 1234");
        testPaymentMethod.setCardholderName("John Doe");
        testPaymentMethod.setExpiryMonth("12");
        testPaymentMethod.setExpiryYear("2025");
        testPaymentMethod.setIsDefault(true);

        paymentMethodDto = new PaymentMethodDto();
        paymentMethodDto.setType(UserPaymentMethod.PaymentType.card);
        paymentMethodDto.setCardNumber("1234567890123456");
        paymentMethodDto.setCardholderName("John Doe");
        paymentMethodDto.setExpiryMonth("12");
        paymentMethodDto.setExpiryYear("2025");
        paymentMethodDto.setIsDefault(true);
    }

    @Test
    void getUserPaymentMethods_ShouldReturnPaymentMethodList() throws Exception {
        List<UserPaymentMethod> methods = Arrays.asList(testPaymentMethod);
        when(paymentMethodService.getUserPaymentMethods(1L)).thenReturn(methods);

        mockMvc.perform(get("/api/users/1/payment-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("card"))
                .andExpect(jsonPath("$[0].cardNumber").value("**** **** **** 1234"));

        verify(paymentMethodService).getUserPaymentMethods(1L);
    }

    @Test
    void addPaymentMethod_ShouldCreatePaymentMethod() throws Exception {
        when(paymentMethodService.addPaymentMethod(eq(1L), any(PaymentMethodDto.class))).thenReturn(testPaymentMethod);

        mockMvc.perform(post("/api/users/1/payment-methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentMethodDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("card"))
                .andExpect(jsonPath("$.cardholderName").value("John Doe"));

        verify(paymentMethodService).addPaymentMethod(eq(1L), any(PaymentMethodDto.class));
    }

    @Test
    void updatePaymentMethod_ShouldUpdatePaymentMethod() throws Exception {
        when(paymentMethodService.updatePaymentMethod(eq(1L), eq(1L), any(PaymentMethodDto.class))).thenReturn(testPaymentMethod);

        mockMvc.perform(put("/api/users/1/payment-methods/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentMethodDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").value("John Doe"));

        verify(paymentMethodService).updatePaymentMethod(eq(1L), eq(1L), any(PaymentMethodDto.class));
    }

    @Test
    void deletePaymentMethod_ShouldDeletePaymentMethod() throws Exception {
        doNothing().when(paymentMethodService).deletePaymentMethod(1L, 1L);

        mockMvc.perform(delete("/api/users/1/payment-methods/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment method deleted successfully"));

        verify(paymentMethodService).deletePaymentMethod(1L, 1L);
    }
}
package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.client.UserAdminClient;
import com.megamart.order_payment_service.dto.AddressDto;
import com.megamart.order_payment_service.dto.AddressRequestDto;
import com.megamart.order_payment_service.dto.PaymentMethodDto;
import com.megamart.order_payment_service.dto.PaymentMethodRequestDto;
import com.megamart.order_payment_service.exception.AddressNotFoundException;
import com.megamart.order_payment_service.exception.PaymentMethodNotFoundException;
import com.megamart.order_payment_service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockUserDataServiceTest {

    @Mock
    private UserAdminClient userAdminClient;

    private MockUserDataService mockUserDataService;

    @BeforeEach
    void setUp() {
        mockUserDataService = new MockUserDataService(userAdminClient);
    }

    @Test
    void getAddress_Success_FromUserService() {
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1L);
        response.put("fullName", "John Doe");
        response.put("addressLine1", "123 Main St");
        response.put("city", "New York");
        response.put("state", "NY");
        response.put("postalCode", "10001");
        response.put("country", "USA");
        response.put("phone", "1234567890");

        when(userAdminClient.getAddressById(1L, 1L)).thenReturn(response);

        AddressDto result = mockUserDataService.getAddress(1L, 1L);

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
        assertEquals("123 Main St", result.getAddressLine1());
    }

    @Test
    void getAddress_Success_FromMockData() {
        when(userAdminClient.getAddressById(anyLong(), anyLong())).thenThrow(new RuntimeException("Service unavailable"));

        AddressDto result = mockUserDataService.getAddress(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void getAddress_NotFound() {
        when(userAdminClient.getAddressById(anyLong(), anyLong())).thenReturn(null);

        assertThrows(AddressNotFoundException.class, () -> mockUserDataService.getAddress(1L, 999L));
    }

    @Test
    void createAddress_Success() {
        AddressRequestDto request = AddressRequestDto.builder()
                .fullName("Jane Doe")
                .addressLine1("456 Oak St")
                .city("Boston")
                .state("MA")
                .postalCode("02101")
                .country("USA")
                .phone("9876543210")
                .build();

        AddressDto result = mockUserDataService.createAddress(1L, request);

        assertNotNull(result);
        assertEquals("Jane Doe", result.getFullName());
        assertEquals("456 Oak St", result.getAddressLine1());
        assertNotNull(result.getId());
    }

    @Test
    void createAddress_InvalidPhone() {
        AddressRequestDto request = AddressRequestDto.builder()
                .fullName("Jane Doe")
                .phone("invalid")
                .build();

        assertThrows(ValidationException.class, () -> mockUserDataService.createAddress(1L, request));
    }

    @Test
    void getPaymentMethod_Success() {
        PaymentMethodDto result = mockUserDataService.getPaymentMethod(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("CARD", result.getType());
        assertEquals("****-****-****-1234", result.getCardNumber());
    }

    @Test
    void getPaymentMethod_NotFound() {
        assertThrows(PaymentMethodNotFoundException.class, () -> mockUserDataService.getPaymentMethod(1L, 999L));
    }

    @Test
    void createPaymentMethod_Card_Success() {
        PaymentMethodRequestDto request = PaymentMethodRequestDto.builder()
                .type("CARD")
                .cardNumber("1234567890123456")
                .cardholderName("Jane Doe")
                .expiryMonth("12")
                .expiryYear("2025")
                .cvv("123")
                .build();

        PaymentMethodDto result = mockUserDataService.createPaymentMethod(1L, request);

        assertNotNull(result);
        assertEquals("CARD", result.getType());
        assertEquals("Jane Doe", result.getCardholderName());
        assertTrue(result.getCardNumber().contains("****"));
    }

    @Test
    void createPaymentMethod_UPI_Success() {
        PaymentMethodRequestDto request = PaymentMethodRequestDto.builder()
                .type("UPI")
                .upiId("user@paytm")
                .build();

        PaymentMethodDto result = mockUserDataService.createPaymentMethod(1L, request);

        assertNotNull(result);
        assertEquals("UPI", result.getType());
        assertEquals("user@paytm", result.getUpiId());
    }

    @Test
    void createPaymentMethod_InvalidCardNumber() {
        PaymentMethodRequestDto request = PaymentMethodRequestDto.builder()
                .type("CARD")
                .cardNumber("invalid")
                .build();

        assertThrows(ValidationException.class, () -> mockUserDataService.createPaymentMethod(1L, request));
    }

    @Test
    void createPaymentMethod_InvalidCvv() {
        PaymentMethodRequestDto request = PaymentMethodRequestDto.builder()
                .type("CARD")
                .cardNumber("1234567890123456")
                .cvv("invalid")
                .build();

        assertThrows(ValidationException.class, () -> mockUserDataService.createPaymentMethod(1L, request));
    }
}
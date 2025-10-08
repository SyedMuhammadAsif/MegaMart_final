package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.client.UserAdminClient;
import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.exception.AddressNotFoundException;
import com.megamart.order_payment_service.exception.PaymentMethodNotFoundException;
import com.megamart.order_payment_service.exception.ValidationException;
import com.megamart.order_payment_service.service.interfaces.UserDataServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class MockUserDataService implements UserDataServiceInterface {

    private final Map<Long, AddressDto> addresses = new HashMap<>();
    private final Map<Long, PaymentMethodDto> paymentMethods = new HashMap<>();
    private final AtomicLong addressIdCounter = new AtomicLong(1);
    private final AtomicLong paymentMethodIdCounter = new AtomicLong(1);

    private final UserAdminClient userAdminClient;

    @Autowired
    public MockUserDataService(UserAdminClient userAdminClient) {
        this.userAdminClient = userAdminClient;
        loadSampleData();
    }

    @Override
    public AddressDto getAddress(Long userId, Long addressId) {
        try {
            // THIS IS THE CORRECTED LINE: Pass both userId and addressId
            Map<String, Object> response = userAdminClient.getAddressById(userId, addressId);

            if (response != null) {
                return AddressDto.builder()
                        .id(Long.valueOf(response.get("id").toString()))
                        .fullName((String) response.get("fullName"))
                        .addressLine1((String) response.get("addressLine1"))
                        .addressLine2((String) response.get("addressLine2"))
                        .city((String) response.get("city"))
                        .state((String) response.get("state"))
                        .postalCode((String) response.get("postalCode"))
                        .country((String) response.get("country"))
                        .phone((String) response.get("phone"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to fetch address from user service: {}. Falling back to local mock data.", e.getMessage());
        }

        // Fallback to local mock data if the service call fails
        AddressDto address = addresses.get(addressId);
        if (address == null || !address.getId().equals(addressId)) {
            throw new AddressNotFoundException(userId.intValue(), addressId);
        }
        return address;
    }

    @Override
    public AddressDto createAddress(Long userId, AddressRequestDto address) {
        validateAddress(address);

        Long newId = addressIdCounter.getAndIncrement();
        AddressDto newAddress = AddressDto.builder()
                .id(newId)
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .phone(address.getPhone())
                .isDefault(address.getIsDefault())
                .build();

        addresses.put(newId, newAddress);
        log.info("Created temporary address for order with ID: {}", newId);
        return newAddress;
    }

    @Override
    public PaymentMethodDto getPaymentMethod(Long userId, Long paymentMethodId) {
        PaymentMethodDto paymentMethod = paymentMethods.get(paymentMethodId);
        if (paymentMethod == null || !paymentMethod.getId().equals(paymentMethodId)) {
            throw new PaymentMethodNotFoundException(userId.intValue(), paymentMethodId);
        }
        return paymentMethod;
    }

    @Override
    public PaymentMethodDto createPaymentMethod(Long userId, PaymentMethodRequestDto paymentMethod) {
        validatePaymentMethod(paymentMethod);

        Long newId = paymentMethodIdCounter.getAndIncrement();
        PaymentMethodDto newPaymentMethod = PaymentMethodDto.builder()
                .id(newId)
                .type(paymentMethod.getType())
                .cardNumber(maskCardNumber(paymentMethod.getCardNumber()))
                .cardholderName(paymentMethod.getCardholderName())
                .expiryMonth(paymentMethod.getExpiryMonth())
                .expiryYear(paymentMethod.getExpiryYear())
                .upiId(paymentMethod.getUpiId())
                .isDefault(paymentMethod.getIsDefault())
                .build();

        paymentMethods.put(newId, newPaymentMethod);
        log.info("Created payment method with ID: {}", newId);
        return newPaymentMethod;
    }

    private void validateAddress(AddressRequestDto address) {
        if (address.getPhone() == null || !address.getPhone().matches("^\\d{10}$")) {
            throw new ValidationException("phone", "Phone number must be exactly 10 digits");
        }
    }

    private void validatePaymentMethod(PaymentMethodRequestDto paymentMethod) {
        if ("CARD".equals(paymentMethod.getType())) {
            if (paymentMethod.getCardNumber() == null || !paymentMethod.getCardNumber().matches("^\\d{16}$")) {
                throw new ValidationException("cardNumber", "Card number must be exactly 16 digits");
            }
            if (paymentMethod.getCvv() == null || !paymentMethod.getCvv().matches("^\\d{3}$")) {
                throw new ValidationException("cvv", "CVV must be exactly 3 digits");
            }
        }
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.contains("*")) {
            return cardNumber;
        }
        if (cardNumber.length() >= 4) {
            String lastFour = cardNumber.substring(cardNumber.length() - 4);
            return "****-****-****-" + lastFour;
        }
        return cardNumber;
    }

    private void loadSampleData() {
        AddressDto address1 = AddressDto.builder()
                .id(1L)
                .fullName("John Doe")
                .addressLine1("123 Main Street")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .phone("1234567890")
                .isDefault(true)
                .build();
        addresses.put(1L, address1);

        PaymentMethodDto paymentMethod1 = PaymentMethodDto.builder()
                .id(1L)
                .type("CARD")
                .cardNumber("****-****-****-1234")
                .cardholderName("John Doe")
                .expiryMonth("12")
                .expiryYear("2025")
                .isDefault(true)
                .build();
        paymentMethods.put(1L, paymentMethod1);

        addressIdCounter.set(2L);
        paymentMethodIdCounter.set(2L);

        log.info("Loaded sample data");
    }
}
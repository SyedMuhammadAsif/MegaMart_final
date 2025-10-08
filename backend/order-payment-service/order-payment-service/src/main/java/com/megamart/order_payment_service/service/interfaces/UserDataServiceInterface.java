package com.megamart.order_payment_service.service.interfaces;

import com.megamart.order_payment_service.dto.*;

public interface UserDataServiceInterface {
    
    AddressDto getAddress(Long userId, Long addressId);
    
    AddressDto createAddress(Long userId, AddressRequestDto address);
    
    PaymentMethodDto getPaymentMethod(Long userId, Long paymentMethodId);
    
    PaymentMethodDto createPaymentMethod(Long userId, PaymentMethodRequestDto paymentMethod);
} 
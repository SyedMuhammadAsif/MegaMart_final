package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.PaymentMethodDto;
import com.megamart.useradminserver.entity.UserPaymentMethod;

import java.util.List;

public interface PaymentMethodService {
    List<UserPaymentMethod> getUserPaymentMethods(Long userId);
    UserPaymentMethod addPaymentMethod(Long userId, PaymentMethodDto paymentMethodDto);
    UserPaymentMethod updatePaymentMethod(Long userId, Long methodId, PaymentMethodDto paymentMethodDto);
    void deletePaymentMethod(Long userId, Long methodId);
}
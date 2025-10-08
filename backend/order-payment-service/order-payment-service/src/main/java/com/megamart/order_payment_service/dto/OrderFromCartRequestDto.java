package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFromCartRequestDto {
    private AddressRequestDto address;
    private PaymentMethodRequestDto paymentMethod;
}
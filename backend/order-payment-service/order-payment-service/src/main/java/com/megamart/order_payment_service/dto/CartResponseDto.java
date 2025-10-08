package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private Long id;
    private String userId;
    private Double total;
    private List<CartItemDto> items;
    private Integer totalItems;
    private Double totalPrice;
}
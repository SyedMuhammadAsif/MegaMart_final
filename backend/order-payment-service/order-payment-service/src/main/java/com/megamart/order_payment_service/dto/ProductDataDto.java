package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDataDto {
    private Long id;
    private String title;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private String brand;
}
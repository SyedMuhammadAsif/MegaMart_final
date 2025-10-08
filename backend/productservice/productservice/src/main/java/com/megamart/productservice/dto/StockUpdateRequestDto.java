package com.megamart.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequestDto {
    private Integer stockChange;
    
    public Integer getStockChange() { return stockChange; }
    public void setStockChange(Integer stockChange) { this.stockChange = stockChange; }
}
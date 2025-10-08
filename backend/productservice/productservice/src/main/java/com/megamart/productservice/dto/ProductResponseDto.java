package com.megamart.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private boolean success;
    private Object data;
    private String message;
    private LocalDateTime timestamp;
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static ProductResponseDto success(Object data, String message) {
        ProductResponseDto response = new ProductResponseDto();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public static ProductResponseDto error(String message) {
        ProductResponseDto response = new ProductResponseDto();
        response.setSuccess(false);
        response.setData(null);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
package com.megamart.order_payment_service.client;

import com.megamart.order_payment_service.dto.ProductResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "productservice")
public interface ProductServiceClient {

    @CircuitBreaker(name = "product-service", fallbackMethod = "serviceFallback")
    @GetMapping("/api/products/{productId}")
    ProductResponseDto getProductById(@PathVariable Long productId);

    @CircuitBreaker(name = "product-service", fallbackMethod = "serviceFallbackMap")
    @PostMapping("/api/products/{productId}/stock")
    Map<String, Object> updateStock(@PathVariable Long productId, @RequestBody Map<String, Integer> request);

    default ProductResponseDto serviceFallback(Long productId, Exception ex) {

        return null;
    }

    default Map<String, Object> serviceFallbackMap(Long productId, Map<String, Integer> request, Exception ex) {
        return Map.of("error", "Product service unavailable");
    }
}
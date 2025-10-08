package com.megamart.order_payment_service.client;

import com.megamart.order_payment_service.dto.CartResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "CartWishlistService")
public interface CartServiceClient {

    @CircuitBreaker(name = "cart-service", fallbackMethod = "serviceFallback")
    @GetMapping("/cart/{userId}")
    CartResponseDto getCart(@PathVariable String userId);

    @CircuitBreaker(name = "cart-service", fallbackMethod = "serviceFallbackVoid")
    @DeleteMapping("/cart/{userId}")
    void clearCart(@PathVariable String userId);

    default CartResponseDto serviceFallback(String userId, Exception ex) {
        return null;
    }

    default void serviceFallbackVoid(String userId, Exception ex) {

    }
}
package com.megamart.order_payment_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;


@FeignClient(name = "user-admin-service")
public interface UserAdminClient {

    @CircuitBreaker(name = "user-service", fallbackMethod = "serviceFallbackAddress")
    @GetMapping("/api/users/{userId}/addresses/{addressId}")
    Map<String, Object> getAddressById(@PathVariable("userId") Long userId, @PathVariable("addressId") Long addressId);

    @CircuitBreaker(name = "user-service", fallbackMethod = "serviceFallback")
    @GetMapping("/api/users/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") Long userId);

    default Map<String, Object> serviceFallbackAddress(Long userId, Long addressId, Exception ex) {

        return Map.of("error", "User service unavailable");
    }

    default Map<String, Object> serviceFallback(Long userId, Exception ex) {

        return Map.of("error", "User service unavailable");
    }
}
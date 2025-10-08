package com.megamart.cartwishlist.client;

import com.megamart.cartwishlist.dto.TokenValidationResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @CircuitBreaker(name = "auth-service", fallbackMethod = "serviceFallback")
    @PostMapping("/api/auth/validate")
    TokenValidationResponseDto validateToken(@RequestHeader("Authorization") String authHeader);

    default TokenValidationResponseDto serviceFallback(String authHeader, Exception ex) {
        return new TokenValidationResponseDto(false, null, null, null);
    }
}
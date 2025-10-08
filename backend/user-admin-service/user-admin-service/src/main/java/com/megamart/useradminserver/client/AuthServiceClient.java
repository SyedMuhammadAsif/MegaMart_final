package com.megamart.useradminserver.client;

import com.megamart.useradminserver.dto.SyncUserRequestDto;
import com.megamart.useradminserver.dto.TokenValidationResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @CircuitBreaker(name = "auth-service", fallbackMethod = "serviceFallbackString")
    @PostMapping("/api/auth/sync-user")
    String syncUser(@RequestBody SyncUserRequestDto request);

    @CircuitBreaker(name = "auth-service", fallbackMethod = "serviceFallback")
    @PostMapping("/api/auth/validate")
    TokenValidationResponseDto validateToken(@RequestHeader("Authorization") String authHeader);

    default String serviceFallbackString(SyncUserRequestDto request, Exception ex) {

        return "Auth service unavailable - user sync failed";
    }

    default TokenValidationResponseDto serviceFallback(String authHeader, Exception ex) {

        return new TokenValidationResponseDto(false, null, null, null);
    }
}
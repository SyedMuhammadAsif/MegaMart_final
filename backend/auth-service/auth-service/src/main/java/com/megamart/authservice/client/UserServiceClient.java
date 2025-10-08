package com.megamart.authservice.client;

import com.megamart.authservice.dto.UserInfoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@FeignClient(name = "user-admin-service")

public interface UserServiceClient {

    @CircuitBreaker(name = "user-service", fallbackMethod = "serviceFallback")
    @GetMapping("/api/users/email/{email}")
    UserInfoDto getUserByEmail(@PathVariable String email);

    default UserInfoDto serviceFallback(String email, Exception ex) {
        return null;
    }
}
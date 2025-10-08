package com.megamart.gateway.controller; // Notice the package name is now correct

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/message")
    public ResponseEntity<Map<String, String>> serviceFallback() {
        Map<String, String> response = Map.of(
                "error", "Service is currently unavailable",
                "message", "Please try again later or contact support"
        );

        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
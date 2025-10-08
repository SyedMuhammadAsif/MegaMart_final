package com.megamart.gateway.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.megamart.gateway.client")
public class FeignConfig {
}
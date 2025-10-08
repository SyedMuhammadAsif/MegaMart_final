package com.megamart.useradminserver.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.megamart.useradminserver.client")
public class FeignConfig {
}
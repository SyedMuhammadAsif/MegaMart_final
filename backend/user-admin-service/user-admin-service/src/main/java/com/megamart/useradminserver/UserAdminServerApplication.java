package com.megamart.useradminserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
public class UserAdminServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(
                UserAdminServerApplication.class, args);
       log.info("User Admin Service running successfully");
    }
}
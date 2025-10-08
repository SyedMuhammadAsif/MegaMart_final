package com.megamart.order_payment_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@Slf4j

public class OrderPaymentServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(OrderPaymentServiceApplication.class, args);
		log.info("Order Payment Service running successfully");
	}

}

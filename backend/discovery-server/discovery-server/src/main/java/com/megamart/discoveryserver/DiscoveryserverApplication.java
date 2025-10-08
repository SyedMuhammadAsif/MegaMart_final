package com.megamart.discoveryserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
@Slf4j
public class DiscoveryserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryserverApplication.class, args);
		log.info("Discovery server running successfully");
	}

}

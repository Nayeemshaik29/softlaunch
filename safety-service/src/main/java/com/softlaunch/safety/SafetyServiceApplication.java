package com.softlaunch.safety;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@ConfigurationPropertiesScan
public class SafetyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafetyServiceApplication.class, args);
	}

}

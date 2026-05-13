package com.zjgsu.whattoeat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WhatToEatApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhatToEatApplication.class, args);
	}

}

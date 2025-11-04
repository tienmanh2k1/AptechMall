package com.aptech.aptechMall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AptechMallApplication {

	public static void main(String[] args) {
		SpringApplication.run(AptechMallApplication.class, args);
	}

}

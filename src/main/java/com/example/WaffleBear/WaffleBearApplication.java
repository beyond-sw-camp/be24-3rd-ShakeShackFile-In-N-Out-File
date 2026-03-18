package com.example.WaffleBear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WaffleBearApplication{

	public static void main(String[] args) {
		SpringApplication.run(WaffleBearApplication.class, args);
	}

}

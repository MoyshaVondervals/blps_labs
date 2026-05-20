package org.moysha.createorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CreateOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreateOrderServiceApplication.class, args);
	}

}

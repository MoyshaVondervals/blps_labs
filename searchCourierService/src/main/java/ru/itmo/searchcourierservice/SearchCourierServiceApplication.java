package ru.itmo.searchcourierservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SearchCourierServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchCourierServiceApplication.class, args);
    }
}

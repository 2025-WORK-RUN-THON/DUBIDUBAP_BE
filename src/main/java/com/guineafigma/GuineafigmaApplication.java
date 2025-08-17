package com.guineafigma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GuineafigmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuineafigmaApplication.class, args);
    }

} 
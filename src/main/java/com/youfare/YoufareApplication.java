package com.youfare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class YoufareApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoufareApplication.class, args);
    }
}

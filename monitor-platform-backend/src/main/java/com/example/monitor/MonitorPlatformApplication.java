package com.example.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonitorPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorPlatformApplication.class, args);
    }
}


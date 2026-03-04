package com.akash.loginsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.akash.loginsystem.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableScheduling
public class LoginSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginSystemApplication.class, args);
    }
}

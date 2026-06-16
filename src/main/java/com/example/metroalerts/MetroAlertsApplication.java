package com.example.metroalerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MetroAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetroAlertsApplication.class, args);
    }
}

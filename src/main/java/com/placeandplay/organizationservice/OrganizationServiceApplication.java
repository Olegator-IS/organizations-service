package com.placeandplay.organizationservice;

import com.placeandplay.organizationservice.config.SSHTunnel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class OrganizationServiceApplication {

    public static void main(String[] args) {
        SSHTunnel.createSSHTunnel(); // Устанавливаем SSH туннель
        SpringApplication.run(OrganizationServiceApplication.class, args);
    }


    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(OrganizationServiceApplication.class);
    }
}

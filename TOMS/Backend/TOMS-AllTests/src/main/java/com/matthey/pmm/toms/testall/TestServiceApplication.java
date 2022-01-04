package com.matthey.pmm.toms.testall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = {"com.matthey.pmm.toms.service",
		"com.matthey.pmm.toms.model", "com.matthey.pmm.toms.repository"})
@EnableJpaRepositories(basePackages = {"com.matthey.pmm.toms.repository"})
@EntityScan (basePackages = {"com.matthey.pmm.toms.model"})
public class TestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestServiceApplication.class, args);
    }    
}

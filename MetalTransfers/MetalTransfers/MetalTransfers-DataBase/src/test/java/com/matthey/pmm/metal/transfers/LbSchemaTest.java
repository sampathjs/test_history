package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.metal.transfers.repository.UserJmFormRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LbSchemaTest.LbSchemaTestConfig.class)
public class LbSchemaTest {

    @Configuration
    @EnableAutoConfiguration
    @AutoConfigureDataJpa
    @ComponentScan(basePackages = {"com.matthey.pmm.endur.database.repository",
            "com.matthey.pmm.metal.transfers.repository"})
    static class LbSchemaTestConfig {
    }

    @Autowired
    UserJmFormRepository userJmFormRepository;

    @Test
    public void contextLoads() {
        System.out.println("running");
    }
}

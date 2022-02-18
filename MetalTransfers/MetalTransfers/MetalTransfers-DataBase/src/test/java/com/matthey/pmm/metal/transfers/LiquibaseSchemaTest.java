package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.endur.database.repository.AbTranRepository;
import com.matthey.pmm.metal.transfers.repository.UserJmFormRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LiquibaseSchemaTest.LbSchemaTestConfig.class)
public class LiquibaseSchemaTest {

    @Configuration
    @EnableAutoConfiguration
    @AutoConfigureDataJpa
    @EnableJpaRepositories(basePackages = {"com.matthey.pmm"})
    @EntityScan(basePackages = {"com.matthey.pmm"})
    @ComponentScan(basePackages = {"com.matthey.pmm"})
    static class LbSchemaTestConfig {
    }

    @Autowired
    AbTranRepository abTranRepository;

    @Autowired
    UserJmFormRepository userJmFormRepository;

    @Test
    public void contextLoads() {
        
    }
}

package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.endur.database.repository.AbTranRepository;
import com.matthey.pmm.endur.database.repository.CurrencyRepository;
import com.matthey.pmm.metal.transfers.repository.UserJmFormRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import tbrugz.sqldump.SQLDump;

import javax.naming.NamingException;
import java.io.IOException;
import java.sql.SQLException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LiquibaseSchemaTest.LbSchemaTestConfig.class)
@Slf4j
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
    CurrencyRepository currencyRepository;

    @Autowired
    UserJmFormRepository userJmFormRepository;

    @Test
    public void contextLoads() throws Exception {
        log.info("context loaded");
        val currencies = currencyRepository.findAll();
        currencies.forEach(c -> log.info("got currency {}", c.getName()));
        SQLDump.main(new String[]{"-propfile=src/test/resources/sqldump/sqldump.properties"});

        log.info("dump completed");
    }
}

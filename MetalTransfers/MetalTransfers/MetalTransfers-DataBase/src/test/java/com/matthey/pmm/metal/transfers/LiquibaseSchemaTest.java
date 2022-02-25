package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.endur.database.repository.CurrencyRepository;
import com.matthey.pmm.metal.transfers.repository.MtAbTranRepository;
import com.matthey.pmm.metal.transfers.repository.UserJmFormRepository;
import com.matthey.pmm.metal.transfers.repository.UserJmMtProcessRepository;
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

import javax.transaction.Transactional;

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

    @Autowired
    MtAbTranRepository mtAbTranRepository;

    @Autowired
    UserJmMtProcessRepository userJmMtProcessRepository;

    @Test
    @Transactional
    public void contextLoads() throws Exception {
        log.info("context loaded");

        val transactions = mtAbTranRepository.findAll();
        transactions.forEach(t -> {
           log.info("got transaction {}", t.getTranNum());
           val tranInfos = t.getAbTranInfos();
           tranInfos.forEach(ti -> {
               log.info("---- got tran info {}", ti.getValue());
           });
        });

        val mtProcesses = userJmMtProcessRepository.findAll();
        mtProcesses.forEach(p -> {
           log.info("got process {}", p.getRunId());
           log.info("--- tran: {}", p.getAbTran().getTranNum());
           log.info("--- personnel: {}", p.getPersonnel().getIdNumber());
        });

//        val currencies = currencyRepository.findAll();
//        currencies.forEach(c -> log.info("got currency {}", c.getName()));
        //SQLDump.main(new String[]{"-propfile=src/test/resources/sqldump/sqldump.properties"});

        log.info("dump completed");
    }
}

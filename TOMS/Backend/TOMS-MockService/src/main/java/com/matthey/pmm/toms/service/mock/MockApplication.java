package com.matthey.pmm.toms.service.mock;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.matthey.pmm.toms.model.DbConstants;
import com.matthey.pmm.toms.service.conversion.IndexConverter;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestIndex;
import com.matthey.pmm.toms.service.mock.testdata.TestOrderComment;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@SpringBootApplication(scanBasePackages = {"com.matthey.pmm.toms.service.mock", "com.matthey.pmm.toms.service",
		"com.matthey.pmm.toms.model", "com.matthey.pmm.toms.repository", "com.matthey.pmm.toms.service"})
@EnableSwagger2
@EnableJpaRepositories(basePackages = {"com.matthey.pmm.toms.repository"})
@EntityScan (basePackages = {"com.matthey.pmm.toms.model"})
public class MockApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockApplication.class, args);
    }

    @Bean
    public Docket api() {
    	String s = DbConstants.SCHEMA_NAME;
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    @Order(value = 0)
    public CommandLineRunner loadPartyTestData(PartyConverter partyConverter) {
      return (args) -> {
    	  TestParty.asList() // legal entities first (LEs are not assigned to another LE)
    	  	.stream()
    	  	.filter(x -> x.idLegalEntity() <= 0)
    	  	.forEach(x -> partyConverter.toManagedEntity(x));
    	  TestParty.asList() // now the business units (everything that has an LE)
    	  	.stream()
    	  	.filter(x -> x.idLegalEntity() > 0)
    	  	.forEach(x -> partyConverter.toManagedEntity(x));
      };
    }
    
    @Bean
    @Order(value = 10)
    public CommandLineRunner loadUserTestData(UserConverter userConverter) {
      return (args) -> {
    	  TestUser.asList() 
    	  	.stream()
    	  	.forEach(x -> userConverter.toManagedEntity(x));
      };
    }
    
    @Bean
    @Order(value = 0)
    public CommandLineRunner loadIndexTestData(IndexConverter indexConverter) {
      return (args) -> {
    	  TestIndex.asList()
    	  	.stream()
    	  	.forEach(x -> indexConverter.toManagedEntity(x));
      };
    }
    
    @Bean
    @Order(value = 20)
    public CommandLineRunner loadOrderCommentTestData(OrderCommentConverter orderCommentConverter) {
      return (args) -> {
    	  TestOrderComment.asList() 
    	  	.stream()
    	  	.forEach(x -> orderCommentConverter.toManagedEntity(x));
      };
    }
}

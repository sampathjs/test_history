package com.matthey.pmm.toms.service.mock;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.matthey.pmm.toms.model.DbConstants;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.IndexConverter;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderLegConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestCreditCheck;
import com.matthey.pmm.toms.service.mock.testdata.TestFill;
import com.matthey.pmm.toms.service.mock.testdata.TestIndex;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestOrderComment;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrderLeg;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@SpringBootApplication(scanBasePackages = {"com.matthey.pmm.toms.service.mock", "com.matthey.pmm.toms.service",
		"com.matthey.pmm.toms.model", "com.matthey.pmm.toms.repository"})
@EnableSwagger2
@EnableJpaRepositories(basePackages = {"com.matthey.pmm.toms.repository"})
@EntityScan (basePackages = {"com.matthey.pmm.toms.model"})
@EnableWebMvc
public class MockApplication implements WebMvcConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(MockApplication.class, args);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("swagger-ui.html")
            .addResourceLocations("classpath:/META-INF/resources/");
        registry
            .addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
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
    public CommandLineRunner loadLimitOrder (ReferenceOrderConverter referenceOrderConverter,
    		LimitOrderConverter limitOrderConverter, FillConverter fillConverter,
    		CreditCheckConverter creditCheckConverter, OrderCommentConverter orderCommentConverter,
    		IndexConverter indexConverter, UserConverter userConverter, PartyConverter partyConverter,
    		ReferenceOrderLegConverter referenceOrderLegConverter) {

      return (args) -> {
    	  TestParty.asList() // legal entities first (LEs are not assigned to another LE)
  	  		.stream()
  	  		.filter(x -> x.idLegalEntity() <= 0)
  	  		.forEach(x -> partyConverter.toManagedEntity(x));
    	  TestParty.asList() // now the business units (everything that has an LE)
  	  		.stream()
  	  		.filter(x -> x.idLegalEntity() > 0)
  	  		.forEach(x -> partyConverter.toManagedEntity(x));
    	  
    	  TestUser.asList() 
  	  		.stream()
  	  		.forEach(x -> userConverter.toManagedEntity(x));
    	  
    	  TestIndex.asList()
  	  		.stream()
  	  		.forEach(x -> indexConverter.toManagedEntity(x));    	  
    	  
    	  TestOrderComment.asList() 
    	  	.stream()
    	  	.forEach(x -> orderCommentConverter.toManagedEntity(x));   
    	  
    	  TestCreditCheck.asList() 
  	  		.stream()
  	  		.forEach(x -> creditCheckConverter.toManagedEntity(x));    	  
   
    	  TestFill.asList() 
   	  		.stream()
   	  		.forEach(x -> fillConverter.toManagedEntity(x));
          
    	  TestLimitOrder.asList() 
    	  	.stream()
    	  	.forEach(x -> limitOrderConverter.toManagedEntity(x));
 
    	  TestReferenceOrderLeg.asList()
    	  	.stream()
    	  	.forEach(x -> referenceOrderLegConverter.toManagedEntity(x));
    	  
    	  TestReferenceOrder.asList() 
  	  		.stream()
  	  		.forEach(x -> referenceOrderConverter.toManagedEntity(x));
      };
    }
}

package com.matthey.pmm.toms.service.mock;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.classmate.TypeResolver;
import com.matthey.pmm.toms.model.DbConstants;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.conversion.DatabaseFileConverter;
import com.matthey.pmm.toms.service.conversion.EmailConverter;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.IndexConverter;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderLegConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ImmutableAttributeCalculationTo;
import com.matthey.pmm.toms.transport.ImmutableCounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableDatabaseFileTo;
import com.matthey.pmm.toms.transport.ImmutableEmailTo;
import com.matthey.pmm.toms.transport.ImmutableFillTo;
import com.matthey.pmm.toms.transport.ImmutableIndexTo;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableOrderCommentTo;
import com.matthey.pmm.toms.transport.ImmutableOrderStatusTo;
import com.matthey.pmm.toms.transport.ImmutableOrderTo;
import com.matthey.pmm.toms.transport.ImmutablePartyTo;
import com.matthey.pmm.toms.transport.ImmutableProcessTransitionTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceTypeTo;
import com.matthey.pmm.toms.transport.ImmutableTickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.ImmutableTickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.ImmutableTickerRefSourceRuleTo;
import com.matthey.pmm.toms.transport.ImmutableTwoListsTo;
import com.matthey.pmm.toms.transport.ImmutableUserTo;
import com.matthey.pmm.toms.transport.IndexTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TwoListsTo;
import com.matthey.pmm.toms.transport.UserTo;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRules;
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
	 private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
	            "classpath:/META-INF/resources/", "classpath:/resources/",
	            "classpath:/static/", "classpath:/public/" };
	
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
        registry.addResourceHandler("/**")
        	.addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);
    }

    @Bean
    public Docket api() {
    	String s = DbConstants.SCHEMA_NAME;
    	TypeResolver typeResolver = new TypeResolver();
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .ignoredParameterTypes(Pageable.class, ModelAndView.class, Page.class, Sort.class, View.class, ModelAndView.class)
                .alternateTypeRules(
                		AlternateTypeRules.newRule(AttributeCalculationTo.class, ImmutableAttributeCalculationTo.class),
                		AlternateTypeRules.newRule(CounterPartyTickerRuleTo.class, ImmutableCounterPartyTickerRuleTo.class),
                		AlternateTypeRules.newRule(CreditCheckTo.class, ImmutableCreditCheckTo.class),
                		AlternateTypeRules.newRule(DatabaseFileTo.class, ImmutableDatabaseFileTo.class),
                		AlternateTypeRules.newRule(EmailTo.class, ImmutableEmailTo.class),
                		AlternateTypeRules.newRule(FillTo.class, ImmutableFillTo.class),
                		AlternateTypeRules.newRule(IndexTo.class, ImmutableIndexTo.class),
                		AlternateTypeRules.newRule(LimitOrderTo.class, ImmutableLimitOrderTo.class),                		
                		AlternateTypeRules.newRule(OrderTo.class, ImmutableOrderTo.class),
                		AlternateTypeRules.newRule(OrderCommentTo.class, ImmutableOrderCommentTo.class),
                		AlternateTypeRules.newRule(OrderStatusTo.class, ImmutableOrderStatusTo.class),
                		AlternateTypeRules.newRule(PartyTo.class, ImmutablePartyTo.class),
                		AlternateTypeRules.newRule(ProcessTransitionTo.class, ImmutableProcessTransitionTo.class),    
                		AlternateTypeRules.newRule(ReferenceOrderLegTo.class, ImmutableReferenceOrderLegTo.class),  
                		AlternateTypeRules.newRule(ReferenceOrderTo.class, ImmutableReferenceOrderTo.class),
                		AlternateTypeRules.newRule(ReferenceTo.class, ImmutableReferenceTo.class),
                		AlternateTypeRules.newRule(ReferenceTypeTo.class, ImmutableReferenceTypeTo.class),
                		AlternateTypeRules.newRule(TickerFxRefSourceRuleTo.class, ImmutableTickerFxRefSourceRuleTo.class),
                		AlternateTypeRules.newRule(TickerPortfolioRuleTo.class, ImmutableTickerPortfolioRuleTo.class),
                		AlternateTypeRules.newRule(TickerRefSourceRuleTo.class, ImmutableTickerRefSourceRuleTo.class),
                		AlternateTypeRules.newRule(TwoListsTo.class, ImmutableTwoListsTo.class),
                		AlternateTypeRules.newRule(UserTo.class, ImmutableUserTo.class)
                )
                ;
    }
                 
    @Bean
    @Transactional
    public CommandLineRunner loadLimitOrder (ReferenceOrderConverter referenceOrderConverter,
    		LimitOrderConverter limitOrderConverter, FillConverter fillConverter,
    		CreditCheckConverter creditCheckConverter, OrderCommentConverter orderCommentConverter,
    		IndexConverter indexConverter, UserConverter userConverter, PartyConverter partyConverter,
    		ReferenceOrderLegConverter referenceOrderLegConverter,
    		DatabaseFileConverter databaseFileConverter, EmailConverter emailConverter) {

      return (args) -> {
    	  
      };
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper()
//          .registerModule(new GuavaModule());
//    }
//    
//    @Bean
//    public Module guavaModule() {
//        return new GuavaModule();
//    }
}

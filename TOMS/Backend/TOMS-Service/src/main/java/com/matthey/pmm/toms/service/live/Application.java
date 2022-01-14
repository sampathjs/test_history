package com.matthey.pmm.toms.service.live;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//import com.fasterxml.jackson.databind.Module;
//import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.matthey.pmm.toms.model.DbConstants;
import com.matthey.pmm.toms.service.live.logic.MasterDataSynchronisation;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = {"com.matthey.pmm.toms.service.live", "com.matthey.pmm.toms.service.common",
		"com.matthey.pmm.toms.service.conversion", 
		"com.matthey.pmm.toms.service.shared",
		"com.matthey.pmm.toms.model", "com.matthey.pmm.toms.repository",
		"com.matthey.pmm.toms.service.logic"})
@EnableSwagger2
@EnableJpaRepositories(basePackages = {"com.matthey.pmm.toms.repository"})
@EntityScan (basePackages = {"com.matthey.pmm.toms.model"})
@EnableWebMvc
@EnableScheduling
public class Application implements WebMvcConfigurer {	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
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
    public CommandLineRunner startupActions (MasterDataSynchronisation masterDataSynchronisation) {
    	return (args) -> { 
    		Logger logger = LogManager.getLogger(Application.class);
    		logger.info("Starting Masterdata Synchronisation on Startup");
    		masterDataSynchronisation.syncMasterdataWithEndur();
    		logger.info("Finished Masterdata Synchronisation on Startup");
    	};
    }
    
    
    @Bean
    public RestTemplate restTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        var sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        var socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        var httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
        
    }
    
//    @Bean
//    public Module guavaModule() {
//        return new GuavaModule();
//    }
}



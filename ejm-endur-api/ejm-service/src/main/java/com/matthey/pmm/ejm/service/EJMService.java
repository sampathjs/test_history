package com.matthey.pmm.ejm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import static com.matthey.pmm.ejm.service.JWTSecurity.AUTH_HEADER;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@SuppressWarnings({"rawtypes", "unchecked"})
@SpringBootApplication(scanBasePackages = "com.matthey.pmm.ejm")
@EnableSwagger2
@EnableCaching
public class EJMService {

    public static final String API_PREFIX = "/ejm";

    public static void main(String[] args) {
        SpringApplication.run(EJMService.class, args);
    }

    @Bean
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(4)).refreshAfterWrite(Duration.ofMinutes(10));
    }

    @Bean
    public CacheManager cacheManager(Caffeine caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        caffeineCacheManager.setCacheLoader(key -> null);
        return caffeineCacheManager;
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

    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }

    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }

    @Bean
    public Docket api() {
        return new Docket(SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.matthey.pmm.ejm"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/")
                .apiInfo(apiInfo())
                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("eJM Endur API")
                .description("REST endpoints used by eJM for retrieving data from Endur" +
                             System.lineSeparator() +
                             "Please retrieve the JWT from endpoint ejm/login, and then click 'Authorize' below and enter 'Bearer [JWT]' ([JWT] is the token just retrieved) for authorization")
                .license(null)
                .licenseUrl(null)
                .termsOfServiceUrl(null)
                .build();
    }

    private ApiKey apiKey() {
        return new ApiKey("JWT", AUTH_HEADER, "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("/ejm/(?!login).*"))
                .build();
    }

    List<SecurityReference> defaultAuth() {
        var authorizationScope = new AuthorizationScope("global", "");
        return List.of(new SecurityReference("JWT", new AuthorizationScope[]{authorizationScope}));
    }
}

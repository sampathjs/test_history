package com.matthey.pmm.toms.service.logic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.tinylog.Logger;

import com.matthey.pmm.toms.service.RestTemplateProvider;

@Component
public class ServiceConnector {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ServiceConnector(RestTemplateProvider restTemplateProvider, @Value("${service.connector.url}") String baseUrl) {
        if (baseUrl.toLowerCase().contains("https")) {
        	this.restTemplate = restTemplateProvider.oAuth2ForwarderRestTemplateForSecureConnection();        	
        } else {
        	this.restTemplate = restTemplateProvider.oAuth2ForwarderRestTemplateForUnsecureConnection();        	        	
        }
        checkArgument(new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(baseUrl),
                      "invalid Service connector URL: " + baseUrl);
        Logger.info("Service connector URL: {}", baseUrl);
        this.baseUrl = baseUrl;
    }
    
	public <T> T get(String url, Class<T> responseType, Object... uriVariables) {
        var result = restTemplate.getForObject(baseUrl + url, responseType, uriVariables);
        checkNotNull(result);
        return result;
    }

    public void put(String url, Object request, Object... uriVariables) {
        restTemplate.put(baseUrl + url, request, uriVariables);
    }

    public void post(String url, Object request, Object... uriVariables) {
        restTemplate.postForLocation(baseUrl + url, request, uriVariables);
    }
    
    public  <T> T postWithResponse(String url,  Class<T> responseType, Object request, Object... uriVariables) {
        ResponseEntity<T> result = restTemplate.postForEntity(baseUrl + url, request, responseType, uriVariables);
        return result.getBody();
    }
}

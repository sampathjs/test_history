package com.matthey.pmm.toms.service.live.logic;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EndurConnector {

    private static final Logger logger = LoggerFactory.getLogger(EndurConnector.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public EndurConnector(RestTemplate restTemplate, @Value("${endur.connector.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        checkArgument(new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(baseUrl),
                      "invalid Endur connector URL: " + baseUrl);
        logger.info("Endur connector URL: {}", baseUrl);
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

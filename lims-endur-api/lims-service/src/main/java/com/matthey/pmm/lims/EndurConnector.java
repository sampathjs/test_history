package com.matthey.pmm.lims;

import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class EndurConnector {

    private static final Logger logger = LoggerFactory.getLogger(EndurConnector.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public EndurConnector(RestTemplate restTemplate, @Value("${endur.connector.url}/lims") String baseUrl) {
        this.restTemplate = restTemplate;
        checkArgument(new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(baseUrl),
                      "invalid Endur connector URL: " + baseUrl);
        logger.info("Endur connector URL: {}", baseUrl);
        this.baseUrl = baseUrl;
    }

    public <T> T get(String url, Class<T> responseType, Object... uriVariables) {
    	logger.info("EndurConnector.get() for response type: {}", responseType.getName());
        var result = restTemplate.getForObject(baseUrl + url, responseType, uriVariables);
        checkNotNull(result);
        return result;
    }
    
    public <T> T post(String url, Class<T> responseType, Object... uriVariables) {
		logger.info("EndurConnector.post() for response type: {}, URL: {}", responseType.getName(), baseUrl + url);
    	var result = restTemplate.postForObject(baseUrl + url, "", responseType, uriVariables);
    	logger.info("EndurConnector.post() result: {}", result);
        checkNotNull(result);
        return result;
    }
    
    public <T> T postBody(String url, Class<T> responseType, Object body, Object... uriVariables) {
    	logger.info("EndurConnector.post() for response type: {}, URL: {}", responseType.getName(), baseUrl + url);
    	var result = restTemplate.postForObject(baseUrl + url, body, responseType, uriVariables);
    	logger.info("EndurConnector.post() result: {}", result);
        checkNotNull(result);
        return result;
    }
}

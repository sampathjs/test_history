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

@Component
public class ServiceConnector implements Supplier<String>{

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private String auth=null;

    public ServiceConnector(RestTemplate restTemplate, @Value("${service.connector.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        checkArgument(new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(baseUrl),
                      "invalid Service connector URL: " + baseUrl);
        Logger.info("Service connector URL: {}", baseUrl);
        restTemplate.getInterceptors().add(new AuthAdditonInterceptor(this));
        this.baseUrl = baseUrl;
    }
    
    public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public <T> T get(String url, Class<T> responseType, String auth, Object... uriVariables) {
		this.auth = auth;
        var result = restTemplate.getForObject(baseUrl + url, responseType, uriVariables);
        checkNotNull(result);
        return result;
    }

    public void put(String url, Object request, String auth, Object... uriVariables) {
		this.auth = auth;
        restTemplate.put(baseUrl + url, request, uriVariables);
    }

    public void post(String url, Object request, String auth, Object... uriVariables) {
		this.auth = auth;
        restTemplate.postForLocation(baseUrl + url, request, uriVariables);
    }
    
    public  <T> T postWithResponse(String url,  Class<T> responseType, String auth, Object request, Object... uriVariables) {
		this.auth = auth;
        ResponseEntity<T> result = restTemplate.postForEntity(baseUrl + url, request, responseType, uriVariables);
        return result.getBody();
    }

	@Override
	public String get() {
		return auth;
	}
}

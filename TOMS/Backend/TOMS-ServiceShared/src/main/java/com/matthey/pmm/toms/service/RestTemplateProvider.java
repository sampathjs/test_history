package com.matthey.pmm.toms.service;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.tinylog.Logger;

import com.matthey.pmm.toms.service.common.OktaClient;

@Component
public class RestTemplateProvider {
	@Autowired
	private OktaClient oktaClient;

    public RestTemplate RestTemplateForSecureConnection() {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = null;
        try {
        	sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException ex) {
        	Logger.error("Error initialising Secure Connection for RestTemplate: " + ex.toString());
        	Logger.error(ex);
        }
        var socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        var httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
//        requestFactory.setConnectionRequestTimeout(300000);
//        requestFactory.setReadTimeout(300000);
        RestTemplate rest = new RestTemplate(requestFactory);        
        return rest;
    }    

	
    public RestTemplate oAuth2ForwarderRestTemplateForSecureConnection() {
        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        SSLContext sslContext = null;
        try {
        	sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException ex) {
        	Logger.error("Error initialising Secure Connection for RestTemplate: " + ex.toString());
        	Logger.error(ex);
        }
        var socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        var httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
//        requestFactory.setConnectionRequestTimeout(300000);
//        requestFactory.setReadTimeout(300000);
        RestTemplate rest = new RestTemplate(requestFactory);
        // forward OAuth2 token if present
        rest.getInterceptors().add((request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
            	// new authentication via Okta
            	request.getHeaders().setBearerAuth(oktaClient.getNewAuth());
                return execution.execute(request, body);
            }
        	request.getHeaders().setBearerAuth(oktaClient.getNewAuth());


//            if (!(authentication.getCredentials() instanceof AbstractOAuth2Token)) {
//                return execution.execute(request, body);
//            }
//
//            AbstractOAuth2Token token = (AbstractOAuth2Token) authentication.getCredentials();
//            request.getHeaders().setBearerAuth(token.getTokenValue());
            return execution.execute(request, body);
        });
        
        return rest;
    }    
    
    public RestTemplate oAuth2ForwarderRestTemplateForUnsecureConnection() {
        RestTemplate rest = new RestTemplate();
        // forward OAuth2 token if present
        rest.getInterceptors().add((request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
            	// new authentication via Okta
            	request.getHeaders().setBearerAuth(oktaClient.getNewAuth());
                return execution.execute(request, body);
            }
        	request.getHeaders().setBearerAuth(oktaClient.getNewAuth());

//            if (!(authentication instanceof OAuth2AuthenticationToken)) {
//                return execution.execute(request, body);
//            }
//            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
//            if (!(token.getPrincipal() instanceof OidcUser)) {
//                return execution.execute(request, body);
//            }
//            DefaultOidcUser principalToken = (DefaultOidcUser)token.getPrincipal();
//            
//            request.getHeaders().setBearerAuth(principalToken.getIdToken().getTokenValue());
            return execution.execute(request, body);
        });
        
        return rest;
    }    
}

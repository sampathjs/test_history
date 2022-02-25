package com.matthey.pmm.toms.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@Order(98)
public class OAuthClientConfiguration {

    // Create the Okta client registration for the service user
    @Bean(name="oktaClientServiceUser")
    ClientRegistration oktaServiceClientRegistration(
            @Value("${spring.security.oauth2.client.provider.oktaClientServiceUser.token-uri}") String tokenUri,
            @Value("${spring.security.oauth2.client.registration.oktaClientServiceUser.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.oktaClientServiceUser.client-secret}") String clientSecret,
            @Value("${spring.security.oauth2.client.registration.oktaClientServiceUser.scope}") String scope,
            @Value("${spring.security.oauth2.client.registration.oktaClientServiceUser.authorization-grant-type}") String authorizationGrantType
    ) {
    	List<String> scopes = Arrays.asList(scope.split(","));
    	scopes = scopes.stream().map(x -> x.trim()).collect(Collectors.toList());
    	System.out.println ("Scopes for oktaClientServiceUser: " + scopes);
        return ClientRegistration.withRegistrationId("oktaServiceUser")        		
                .tokenUri(tokenUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope(scopes.toArray(new String[scopes.size()]))
                .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                .build();
    }
    
    // Create the Okta client registration for the manual login
    @Bean(name="oktaClientManualUser")
    ClientRegistration oktaManualClientRegistration(
            @Value("${spring.security.oauth2.client.registration.oktaClientManualUser.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.oktaClientManualUser.client-secret}") String clientSecret,
            @Value("${spring.security.oauth2.client.registration.oktaClientManualUser.scope}") String scope,
            @Value("${spring.security.oauth2.client.registration.oktaClientManualUser.authorization-grant-type}") String authorizationGrantType,
            @Value("${spring.security.oauth2.client.provider.oktaClientManualUser.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.client.registration.oktaClientManualUser.redirect-uri-template}") String redirectUri,
            @Value("${spring.security.oauth2.client.provider.oktaClientManualUser.user-info-uri}") String userInfoUri,
            @Value("${spring.security.oauth2.client.provider.oktaClientManualUser.jwk-set-uri}") String jwkSetUri
    ) {
    	List<String> scopes = Arrays.asList(scope.split(","));
    	scopes = scopes.stream().map(x -> x.trim()).collect(Collectors.toList());
    	System.out.println ("Scopes for okta client manual user: " + scopes);
    	
        return ClientRegistrations.fromOidcIssuerLocation(issuerUri)                 
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUriTemplate(redirectUri)
                .scope(scopes.toArray(new String[scopes.size()]))
                .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
                .userInfoUri(userInfoUri)
                .userInfoAuthenticationMethod(AuthenticationMethod.HEADER)
                .jwkSetUri(jwkSetUri)
                .build();
    }

    // Create the client registration repository
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
    		@Qualifier("oktaClientServiceUser") ClientRegistration oktaClientRegistration,
    		@Qualifier("oktaClientManualUser") ClientRegistration oktaManualClientRegistration) {
        return new InMemoryClientRegistrationRepository(oktaManualClientRegistration, oktaClientRegistration);
    }

    // Create the authorized client service
    @Bean
    public OAuth2AuthorizedClientService auth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    // Create the authorized client manager and service manager using the
    // beans created and configured above
    @Bean
    public AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager (
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

}
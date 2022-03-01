package com.matthey.pmm.toms.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

@Component
@Order(100)
public class OktaClient {
	@Autowired
	private AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager;
		
    @Qualifier("oktaClientServiceUser") 
    @Autowired
    protected ClientRegistration oktaServiceClientRegistration;
	
	public String getNewAuth() {
		////////////////////////////////////////////////////
		//  STEP 1: Retrieve the authorized JWT
		////////////////////////////////////////////////////
		Authentication principal = new AnonymousAuthenticationToken
			    ("key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		
		// Build an OAuth2 request for the Okta provider
		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(oktaServiceClientRegistration.getRegistrationId())
				.principal(principal)
				.build();
		
		// Perform the actual authorization request using the authorized client service and authorized client
		// manager. This is where the JWT is retrieved from the Okta servers.
		OAuth2AuthorizedClient authorizedClient = this.authorizedClientServiceAndManager.authorize(authorizeRequest);

		// Get the token from the authorized client object
		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();		
		
		String authValue =  accessToken.getTokenValue();
		Logger.info("Generated Auth String: " + authValue);
		return authValue;
	}
}

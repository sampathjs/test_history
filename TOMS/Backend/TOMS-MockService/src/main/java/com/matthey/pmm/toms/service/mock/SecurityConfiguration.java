package com.matthey.pmm.toms.service.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.matthey.pmm.toms.service.TomsOAuth2EnrichedUserService;


@Configuration
@Order(99)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	protected static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";
    
    @Qualifier("oktaClientManualUser") 
    @Autowired
    protected ClientRegistration oktaManualClientRegistration;
    
    @Autowired
    protected TomsOAuth2EnrichedUserService userService;
    
    @Autowired
    protected AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager;
    
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	String manClientId = oktaManualClientRegistration.getRegistrationId();
    	String manualClientUrl = manClientId;
    	String fullUrl = AUTHORIZATION_REQUEST_BASE_URI + "/" + manualClientUrl;    	
        http.authorizeRequests()
        		.anyRequest().authenticated()
        		.and()
        	.oauth2Login()
        		.loginPage(fullUrl)
        		.userInfoEndpoint()
        			.userService(userService)
        			.and()
        		.and()
        	.oauth2ResourceServer().jwt()
        	;
        http.headers().frameOptions().disable();
        http.csrf().disable();
        http.logout().logoutSuccessUrl("/");
    }
    

}

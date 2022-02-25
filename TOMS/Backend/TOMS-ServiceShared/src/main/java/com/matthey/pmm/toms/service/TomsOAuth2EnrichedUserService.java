package com.matthey.pmm.toms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.repository.UserRepository;

@Component
public class TomsOAuth2EnrichedUserService extends DefaultOAuth2UserService {
	@Autowired
	private UserRepository userRepo;
	
	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User fromSuperClass = super.loadUser(userRequest);
		System.out.println(fromSuperClass.getAttributes());
		
		return fromSuperClass;
		
	}
}

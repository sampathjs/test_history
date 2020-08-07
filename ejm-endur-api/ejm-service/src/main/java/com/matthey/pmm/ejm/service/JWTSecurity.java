package com.matthey.pmm.ejm.service;

import org.springframework.security.authentication.AuthenticationManager;

public class JWTSecurity {

    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_PREFIX = "Bearer ";

    public final AuthenticationManager authenticationManager;
    public final String secret;

    public JWTSecurity(AuthenticationManager authenticationManager, String secret) {
        this.authenticationManager = authenticationManager;
        this.secret = secret;
    }
}

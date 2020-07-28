package com.matthey.pmm.ejm.service;

import com.auth0.jwt.JWT;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static com.matthey.pmm.ejm.service.JWTSecurity.AUTH_HEADER;
import static com.matthey.pmm.ejm.service.JWTSecurity.AUTH_PREFIX;

public class JWTAuthenticationFilter extends BasicAuthenticationFilter {

    private final JWTSecurity jwtSecurity;

    public JWTAuthenticationFilter(JWTSecurity jwtSecurity) {
        super(jwtSecurity.authenticationManager);
        this.jwtSecurity = jwtSecurity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        UsernamePasswordAuthenticationToken authentication = null;
        try {
            authentication = getAuthentication(request);
        } catch (Exception ignored) {
        }

        if (authentication == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(AUTH_PREFIX)) {
            return null;
        }
        var user = JWT.require(HMAC512(jwtSecurity.secret.getBytes()))
                .build()
                .verify(header.replace(AUTH_PREFIX, ""))
                .getSubject();
        return user == null ? null : new UsernamePasswordAuthenticationToken(user, null, Set.of());
    }
}
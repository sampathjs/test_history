package com.matthey.pmm.ejm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@EnableWebSecurity
public class SecurityConfiguration {

    private final List<String> allowedIPAddresses;

    public SecurityConfiguration(@Value("${allowed.ip.addresses:}") List<String> allowedIPAddresses) {
        this.allowedIPAddresses = allowedIPAddresses;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String genIPFilter() {
        return allowedIPAddresses.stream().map(ip -> "hasIpAddress('" + ip + "')").collect(Collectors.joining(" or "));
    }

    @Configuration
    @Order(1)
    public class APILoginConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf()
                    .disable()
                    .antMatcher("/ejm/login")
                    .authorizeRequests()
                    .anyRequest()
                    .access(allowedIPAddresses.isEmpty() ? "permitAll()" : genIPFilter())
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }
    }

    @Configuration
    @Order(2)
    public class APISecurityConfiguration extends WebSecurityConfigurerAdapter {

        private final String jwtSecret;

        public APISecurityConfiguration(@Value("${jwt.secret}") String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            var jwtSecurity = new JWTSecurity(authenticationManager(), jwtSecret);
            http.csrf()
                    .disable()
                    .antMatcher("/ejm/**")
                    .authorizeRequests()
                    .anyRequest()
                    .access(allowedIPAddresses.isEmpty() ? "isFullyAuthenticated()" : genIPFilter())
                    .and()
                    .addFilter(new JWTAuthenticationFilter(jwtSecurity))
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Bean
        public JWTSecurity jwtSecurity() throws Exception {
            return new JWTSecurity(authenticationManager(), jwtSecret);
        }
    }

    @Configuration
    @Order(3)
    public class SwaggerSecurityConfiguration extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.httpBasic()
                    .and()
                    .csrf()
                    .disable()
                    .authorizeRequests()
                    .anyRequest()
                    .access(allowedIPAddresses.isEmpty() ? "isFullyAuthenticated()" : genIPFilter());
        }
    }
}

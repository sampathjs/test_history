package com.matthey.pmm.toms.service.mock;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(99)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests()
//                .anyRequest()
//                .access("true")
//                .and()
//                .csrf()
//                .disable();
        http.authorizeRequests()
        	.anyRequest().access("hasIpAddress('::1') or hasIpAddress('127.0.0.1')")
        	.and().csrf().disable();        
    }
}

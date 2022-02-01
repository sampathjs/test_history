package com.matthey.pmm.toms.service.live;

import com.google.common.base.Stopwatch;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

@Component
@WebFilter("/**")
public class ServiceRequestFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String method = httpServletRequest.getMethod();
        String uri = httpServletRequest.getRequestURI();
        Logger.info("Processing HTTP " + method + " " + httpServletRequest.getRequestURL());        
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            chain.doFilter(request, response);
        } catch (ServletException  | IOException e) {
            Logger.error("an error has occurred: " + e.getMessage(), e);
        } finally {
            stopwatch.stop();
            Logger.info("finished request {} {} within {} ms", method, uri, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void destroy() {
    }
}

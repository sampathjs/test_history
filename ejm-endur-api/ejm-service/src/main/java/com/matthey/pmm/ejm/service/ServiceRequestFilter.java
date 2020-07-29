package com.matthey.pmm.ejm.service;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
@WebFilter("/**")
public class ServiceRequestFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpServletRequest = (HttpServletRequest) request;
        var method = httpServletRequest.getMethod();
        var uri = httpServletRequest.getRequestURI();
        var user = httpServletRequest.getUserPrincipal();

        var stopwatch = Stopwatch.createStarted();
        try {
            chain.doFilter(request, response);
        } finally {
            stopwatch.stop();
            logger.info("finished request {} {} by {} within {} ms",
                        method,
                        uri,
                        user == null ? "anonymous" : user.getName(),
                        stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void destroy() {
    }
}

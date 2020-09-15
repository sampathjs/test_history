package com.matthey.pmm;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Component
@WebFilter("/**")
public class ConnectorRequestFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(ConnectorRequestFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String method = httpServletRequest.getMethod();
        String uri = httpServletRequest.getRequestURI();

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("an error has occurred: " + e.getMessage(), e);
        } finally {
            stopwatch.stop();
            logger.info("finished request {} {} within {} ms", method, uri, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void destroy() {
    }
}

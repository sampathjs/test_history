package com.matthey.pmm.toms.service.logic;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.tinylog.Logger;

/*
 * History:
 * 2022-02-17   V1.0  jwaechter   - Initial Version
 *                                  
 */

public class AuthAdditonInterceptor implements ClientHttpRequestInterceptor {

	protected static final Charset UTF_8 = Charset.forName("UTF-8");

	protected final Supplier<String> authorisation;

	/**
	 * Create a new interceptor which adds a bearer to all requests.
	 */
	public AuthAdditonInterceptor(final Supplier<String> authorisation) {
		this.authorisation = authorisation;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
	    ClientHttpRequestExecution execution) throws IOException {
		request.getHeaders().add("Authorization", authorisation.get());
		Logger.info("Added header: " + request.getHeaders().get("Authorization"));
		return execution.execute(request, body);
	}
}

package com.matthey.pmm.connector.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class EndurConnectorSharedAppConfig {
	
	@Bean
	@Qualifier("PrettyFormatter")
	public ObjectMapper getPrettyFormatter () {
		ObjectMapper prettyFormatter = new ObjectMapper();
		prettyFormatter.enable(SerializationFeature.INDENT_OUTPUT);
		// TODO: Generic date format and other normalisation features?
		return prettyFormatter;
	}
}
package com.matthey.pmm.toms.service.live.logic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

@Component
public class ValidationRuleRetrieval {
    private static final Logger logger = LoggerFactory.getLogger(ValidationRuleRetrieval.class);
	
	@Autowired
	private EndurConnector endurConnector;
	
	@Autowired 
	private ReferenceRepository refRepo;
	
	@Autowired 
	private ReferenceConverter refConverter;
	
			
	public List<CounterPartyTickerRuleTo> retrieveCounterPartyTickerRules () {
		logger.info("Starting Retrieving Counter Party Ticker Rules from Endur");
		try {
			List<Reference> entities = refRepo.findAll();
			List<ReferenceTo> references = entities.stream()
					.map(x -> refConverter.toTo(x))
					.collect(Collectors.toList());
			
			List<CounterPartyTickerRuleTo> rules = Arrays.asList(endurConnector.postWithResponse("/toms/endur/counterPartyTickerRule", CounterPartyTickerRuleTo[].class,
					references));
			logger.info("Finished Retrieving Counter Party Ticker Rules from Endur");
			return rules;
		} catch (Exception ex) {
			logger.error("Error while retrieving Counter Party Ticker Rules from Endur Connector: " + ex.getMessage());
			for (StackTraceElement ste : ex.getStackTrace()) {
				logger.error(ste.toString());
			}
			throw ex;
		} 
	}

}

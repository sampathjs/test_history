package com.matthey.pmm.toms.service.live;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.impl.MetadataControllerImpl;
import com.matthey.pmm.toms.service.live.logic.ValidationRuleRetrieval;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;

import io.swagger.annotations.ApiOperation;


@RestController
public class MetadataController extends MetadataControllerImpl {
	@Autowired
	private ValidationRuleRetrieval validationRuleRetriever;
	
    @ApiOperation("Retrieval of the mapping between counter parties, tickers, metal locations, metal forms and accounts")
	public Set<CounterPartyTickerRuleTo> getCounterPartyTickerRules () {
    	return new HashSet<>(validationRuleRetriever.retrieveCounterPartyTickerRules());
    }
}

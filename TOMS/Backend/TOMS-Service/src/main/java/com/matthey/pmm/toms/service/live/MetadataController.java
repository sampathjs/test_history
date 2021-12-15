package com.matthey.pmm.toms.service.live;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.impl.MetadataControllerImpl;
import com.matthey.pmm.toms.service.live.logic.ValidationRuleRetrieval;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;

import io.swagger.annotations.ApiOperation;


@RestController
public class MetadataController extends MetadataControllerImpl {
	@Autowired
	private ValidationRuleRetrieval validationRuleRetriever;
	
    @ApiOperation("Retrieval of the mapping between counter parties, tickers, metal locations, metal forms and accounts")
	public Set<CounterPartyTickerRuleTo> getCounterPartyTickerRules () {
    	return new HashSet<>(validationRuleRetriever.retrieveCounterPartyTickerRules());
    }
    
    @ApiOperation("Retrieval of the mapping between portfolio, party, ticker and index")
	public Set<TickerPortfolioRuleTo> getTickerPortfolioRules () {
    	return new HashSet<>(validationRuleRetriever.retrieveTickerPortfolioRules());    	
    }
}

package com.matthey.pmm.toms.service.mock;

import java.util.HashSet;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.impl.MetadataControllerImpl;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet1;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet2;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet3;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet4;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet5;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet6;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet7;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet8;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerFxRefSourceRule;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerPortfolioRule;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerRefSourceRule;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockMetadataController extends MetadataControllerImpl {
    @ApiOperation("Retrieval of the mapping between counter parties, tickers, metal locations, metal forms and accounts")
	public Set<CounterPartyTickerRuleTo> getCounterPartyTickerRules (
			@ApiParam(value = "Optional restriction to retrieve rules for a certain party only, default = all", example = "20001", required = false) @RequestParam(required=false) Long idCounterparty,
			@ApiParam(value = "Optional retriction to not populate the display string attributes, default= with display string", example = "true", required = false) @RequestParam(required=false) Boolean includeDisplayStrings) {
    	Set<CounterPartyTickerRuleTo> rules = new HashSet<>(TestCounterPartyTickerRuleSet1.values().length*10);
    	rules.addAll(TestCounterPartyTickerRuleSet1.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet2.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet3.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet4.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet5.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet6.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet7.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet8.asList());

    	rules = filterCounterPartyTickerRules (rules, idCounterparty, includeDisplayStrings);
    	return rules;
    }
    
    @ApiOperation("Retrieval of the mapping between portfolio, party, ticker and index")
	public Set<TickerPortfolioRuleTo> getTickerPortfolioRules () {
    	return new HashSet<>(TestTickerPortfolioRule.asList());
    }
    
    @ApiOperation("Retrieval of the mapping between ticker, index and reference sources")
	public Set<TickerRefSourceRuleTo> getTickerRefSourceRules () {
    	return new HashSet<>(TestTickerRefSourceRule.asList());    	
    }
    
    @ApiOperation("Retrieval of the mapping between ticker, index, leg settle currency and reference sources")
	public Set<TickerFxRefSourceRuleTo> getTickerFxRefSourceRules () {
    	return new HashSet<>(TestTickerFxRefSourceRule.asList());    	 
    }
}

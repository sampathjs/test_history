package com.matthey.pmm.toms.service.mock;

import java.util.HashSet;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.matthey.pmm.toms.service.mock.testdata.TestTickerPortfolioRule;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;

import io.swagger.annotations.ApiOperation;

@RestController
public class MockMetadataController extends MetadataControllerImpl {
    @ApiOperation("Retrieval of the mapping between counter parties, tickers, metal locations, metal forms and accounts")
	public Set<CounterPartyTickerRuleTo> getCounterPartyTickerRules () {
    	Set<CounterPartyTickerRuleTo> rules = new HashSet<>(TestCounterPartyTickerRuleSet1.values().length*10);
    	rules.addAll(TestCounterPartyTickerRuleSet1.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet2.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet3.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet4.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet5.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet6.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet7.asList());
    	rules.addAll(TestCounterPartyTickerRuleSet8.asList());
    	return rules;
    }
    
    @ApiOperation("Retrieval of the mapping between portfolio, party, ticker and index")
	public Set<TickerPortfolioRuleTo> getTickerPortfolioRules () {
    	return new HashSet<>(TestTickerPortfolioRule.asList());
    }
}

package com.matthey.pmm.toms.service.live.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.impl.MetadataControllerImpl;
import com.matthey.pmm.toms.service.live.logic.ValidationRuleRetrieval;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@RestController
public class MetadataController extends MetadataControllerImpl {
	@Autowired
	private ValidationRuleRetrieval validationRuleRetriever;
	
    @ApiOperation("Retrieval of the mapping between counter parties, tickers, metal locations, metal forms and accounts")
	public Set<CounterPartyTickerRuleTo> getCounterPartyTickerRules (
			@ApiParam(value = "Optional restriction to retrieve rules for a certain party only, default = all", example = "20001", required = false) @RequestParam(required=false) Long idCounterparty,
			@ApiParam(value = "Optional retriction to not populate the display string attributes, default= with display string", example = "true", required = false) @RequestParam(required=false) Boolean includeDisplayStrings) {
    	Set<CounterPartyTickerRuleTo> rules = new HashSet<>(validationRuleRetriever.retrieveCounterPartyTickerRules());
    	rules = filterCounterPartyTickerRules (rules, idCounterparty, includeDisplayStrings);
    	return rules;
    }
    
    @ApiOperation("Retrieval of the mapping between portfolio, party, ticker and index")
	public Set<TickerPortfolioRuleTo> getTickerPortfolioRules (
			@ApiParam(value = "Optional retriction to not populate the display string attributes, default= with display string", example = "true", required = false) @RequestParam(required=false) Boolean includeDisplayStrings) {
    	Set<TickerPortfolioRuleTo> rules = new HashSet<>(validationRuleRetriever.retrieveTickerPortfolioRules());
    	rules = filterTickerPortfolioRuleToRules(rules, includeDisplayStrings);
    	return rules;
    }
    
    @ApiOperation("Retrieval of the mapping between ticker, index and reference sources")
	public Set<TickerRefSourceRuleTo> getTickerRefSourceRules () {
    	return new HashSet<>(validationRuleRetriever.retrieveTickerRefSourceRules());    	
    }
    
    @ApiOperation("Retrieval of the mapping between ticker, index, leg settle currency and reference sources")
	public Set<TickerFxRefSourceRuleTo> getTickerFxRefSourceRules () {
    	return new HashSet<>(validationRuleRetriever.retrieveTickerFxRefSourceRules());
    }
}

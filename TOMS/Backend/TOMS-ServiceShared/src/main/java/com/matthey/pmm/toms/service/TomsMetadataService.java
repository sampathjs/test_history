package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Metadata"}, description = "APIs for providing process related metadata")
@RequestMapping(API_PREFIX)
public interface TomsMetadataService {
    @Cacheable({"ProcessTransiton"})
    @ApiOperation("Process Status Transition Data Retrieval")
	@GetMapping("/processTransition")
	public Set<ProcessTransitionTo> getProcessTransitions (
			@ApiParam(value = "Transition Category Id, 0 or null = all ", example = "111", required = false) @RequestParam(required=false) Long referenceCategoryId);

    @Cacheable({"AttributeCalculation"})
    @ApiOperation("Retrieval of the calculation logic for attributes of different entites, especially orders")
	@GetMapping("/attributeCalculation")
	public Set<AttributeCalculationTo> getAttributeCalculations (
			@ApiParam(value = "Optional class name to restrict results for a single class", example = "com.matthey.pmm.toms.transport.ImmutableLimitOrderTo", required = false) @RequestParam(required=false) String className);

    @Cacheable({"CounterPartyTickerRules"})
    @ApiOperation("Retrieval of the mapping between counter parties, tickers, metal locations, metal forms and accounts")
	@GetMapping("/counterPartyTickerRules")
	public Set<CounterPartyTickerRuleTo> getCounterPartyTickerRules ();
    
    @Cacheable({"TickerPortfolioRules"})
    @ApiOperation("Retrieval of the mapping between portfolio, party, ticker and index")
	@GetMapping("/tickerPortfolioRules")
	public Set<TickerPortfolioRuleTo> getTickerPortfolioRules ();
    
    @Cacheable({"TickerRefSourceRules"})
    @ApiOperation("Retrieval of the mapping between ticker, index and reference sources")
	@GetMapping("/tickerRefSourceRules")
	public Set<TickerRefSourceRuleTo> getTickerRefSourceRules ();
}

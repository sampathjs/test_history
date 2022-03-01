package com.matthey.pmm.toms.service.common;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.service.logic.ServiceConnector;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

import org.tinylog.Logger;

/**
 * Service using orders and validation rules as input and returning the values of certain Endur fields to be populated.
 * @author jwaechter
 * @version 1.0
 */

@Service
@Transactional
public class DerivedDataService {	
	/**
	 * Connector used to connect to other instances of the service to load the validation rules.
	 */
	@Autowired
	protected ServiceConnector serviceConnector;
	
	@Autowired 
	protected IndexRepository indexRepo;
	
	public IndexEntity getReferenceOrderLegIndexFromTickerRefSourceRule (final ReferenceOrder referenceOrder, ReferenceOrderLeg leg) {
		List<TickerRefSourceRuleTo> rules = getTickerRefSourceRules();
    	List<TickerRefSourceRuleTo> filteredRules = rules.stream()
        		.filter(x -> x.idRefSource() == (leg.getRefSource() != null?leg.getRefSource().getId():0)
        			&&       x.idTicker() == (referenceOrder.getTicker() != null?referenceOrder.getTicker().getId():0l))
        		.collect(Collectors.toList());
    	if (filteredRules.size() == 0) {
    		String msg = "Can not identify a matching index for the provided combination of reference source from leg '" 
    				+ leg + "' and order " + referenceOrder;
    		Logger.error(msg);
    		throw new RuntimeException(msg);
    	}
    	return indexRepo.findById(filteredRules.get(0).idIndex()).get();
	}
	
	
	public List<TickerRefSourceRuleTo> getTickerRefSourceRules() {
		List<TickerRefSourceRuleTo> rules = Arrays.asList(
	    		serviceConnector.get(API_PREFIX + "/tickerRefSourceRules", TickerRefSourceRuleTo[].class));
		return rules;
	}

	public List<TickerPortfolioRuleTo> getTickerPortfolioRules() {
		List<TickerPortfolioRuleTo> rules = Arrays.asList(
	    		serviceConnector.get(API_PREFIX + "/tickerPortfolioRules", TickerPortfolioRuleTo[].class));
		return rules;
	}

	public List<TickerFxRefSourceRuleTo> getTickerFxRefSourceRules() {
		List<TickerFxRefSourceRuleTo> rules = Arrays.asList(
	    		serviceConnector.get(API_PREFIX + "/tickerFxRefSourceRules", TickerFxRefSourceRuleTo[].class));
		return rules;
	}
	

	public List<CounterPartyTickerRuleTo> getCounterPartyTickerRules(long idExternalBu) {
		List<CounterPartyTickerRuleTo> rules = Arrays.asList(
	    		serviceConnector.get(API_PREFIX + "/counterPartyTickerRules?idCounterparty={idCounterparty}", CounterPartyTickerRuleTo[].class, 
	    				idExternalBu));
		return rules;
	}
}

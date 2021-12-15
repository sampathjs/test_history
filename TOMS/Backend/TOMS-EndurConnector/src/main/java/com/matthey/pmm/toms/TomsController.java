package com.matthey.pmm.toms;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.AbstractReferenceService;
import com.matthey.pmm.toms.service.BuySellService;
import com.matthey.pmm.toms.service.CurrenciesService;
import com.matthey.pmm.toms.service.IndexService;
import com.matthey.pmm.toms.service.MetalFormService;
import com.matthey.pmm.toms.service.MetalLocationService;
import com.matthey.pmm.toms.service.PartyService;
import com.matthey.pmm.toms.service.PortfolioService;
import com.matthey.pmm.toms.service.QuantityUnitService;
import com.matthey.pmm.toms.service.RefSourceService;
import com.matthey.pmm.toms.service.TickerService;
import com.matthey.pmm.toms.service.UserService;
import com.matthey.pmm.toms.service.ValidationRuleService;
import com.matthey.pmm.toms.service.YesNoService;
import com.matthey.pmm.toms.service.misc.ReportBuilderHelper;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ImmutableCounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TwoListsTo;
import com.matthey.pmm.toms.transport.UserTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

//import com.olf.openrisk.application.Session;

@RestController
@RequestMapping("/toms/endur/")
public class TomsController {
    private final Session session;

    public TomsController(Session session) {
        this.session = session;
    }
    
    /**
     * Returns party instances that are different or new compared to the provided list of knownParties. 
     * @param knownParties
     * @return
     */
    @PostMapping("parties")
    public List<PartyTo> retrieveEndurPartyList (@RequestBody List<PartyTo> knownParties) {
    	PartyService service = new PartyService (session);
    	return service.createToListDifference(knownParties);
    }
    
    /**
     * Returns user instances that are different or new compared to the provided list of known Users (listOne in knownUsersAndPortfolios).
     * Mapping of the portfolios is done via the provided portfolios on listTwo in knownUsersAndPortfolios. 
     * @param knownUsersAndPortfolios
     * @return
     */
    @PostMapping("users")
    public List<UserTo> retrieveEndurUserList (@RequestBody TwoListsTo<UserTo, ReferenceTo> knownUsersAndPortfolios) {
    	UserService service = new UserService(session, knownUsersAndPortfolios.listTwo());
    	return service.createToListDifference(knownUsersAndPortfolios.listOne());
    }
    
    @PostMapping("references")
    public List<ReferenceTo> retrieveReferences (@RequestBody List<ReferenceTo> knownReferenceData) {
    	// we are receiving a list with all references (of all reference types) so we split them up by type 
    	// and process each type separately and then merge back all sublists for the return value;
    	List<ReferenceTo> globalDiffList = new ArrayList<>(knownReferenceData.size()+10);
    	
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new BuySellService(session), Arrays.asList(DefaultReferenceType.BUY_SELL));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new CurrenciesService(session), Arrays.asList(DefaultReferenceType.CCY_CURRENCY));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new IndexService(session), Arrays.asList(DefaultReferenceType.INDEX_NAME));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new MetalFormService(session), Arrays.asList(DefaultReferenceType.METAL_FORM));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new MetalLocationService(session), Arrays.asList(DefaultReferenceType.METAL_LOCATION));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new PortfolioService(session), Arrays.asList(DefaultReferenceType.PORTFOLIO));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new QuantityUnitService(session), Arrays.asList(DefaultReferenceType.QUANTITY_UNIT));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new RefSourceService(session), Arrays.asList(DefaultReferenceType.REF_SOURCE));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new TickerService(session), Arrays.asList(DefaultReferenceType.TICKER));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new YesNoService(session), Arrays.asList(DefaultReferenceType.YES_NO));
    	
    	return globalDiffList;    	
    }
    
    @PostMapping("counterPartyTickerRule")
    public List<CounterPartyTickerRuleTo> retrieveCounterPartyTickerRules (List<ReferenceTo> references) {
    	ValidationRuleService validationRuleService = new ValidationRuleService(session);
    	return validationRuleService.getCounterPartyTickerRules(references);
    }

    @PostMapping("tickerPortfolioRule")
    public List<TickerPortfolioRuleTo> retrieveTickerPortfolioRules (List<ReferenceTo> references) {
    	ValidationRuleService validationRuleService = new ValidationRuleService(session);
    	return validationRuleService.getTickerPortfolioRules(references);
    }

	private void addReferenceDataDiff(List<ReferenceTo> knownReferenceData, List<ReferenceTo> globalDiffList,
			AbstractReferenceService service, List<DefaultReferenceType> expectedTypes) {
		globalDiffList.addAll(service.createToListDifference(knownReferenceData.stream()
    			.filter(x -> expectedTypes.stream().map( y -> y.getEntity().id()).collect(Collectors.toList()).contains(x.idType()))
    			.collect(Collectors.toList())));
	}
    
    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }
}

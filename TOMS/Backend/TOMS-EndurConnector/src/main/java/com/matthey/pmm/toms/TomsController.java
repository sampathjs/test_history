package com.matthey.pmm.toms;


import java.util.ArrayList;
import java.util.List;
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
import com.matthey.pmm.toms.service.YesNoService;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.TwoListsTo;
import com.matthey.pmm.toms.transport.UserTo;
import com.olf.openrisk.application.Session;

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
    	
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new BuySellService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new CurrenciesService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new IndexService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new MetalFormService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new MetalLocationService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new PortfolioService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new QuantityUnitService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new RefSourceService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new TickerService(session));
    	addReferenceDataDiff(knownReferenceData, globalDiffList, new YesNoService(session));
    	
    	return globalDiffList;    	
    }

	private void addReferenceDataDiff(List<ReferenceTo> knownReferenceData, List<ReferenceTo> globalDiffList,
			AbstractReferenceService service) {
		globalDiffList.addAll(service.createToListDifference(knownReferenceData.stream()
    			.filter(x -> x.idType() == DefaultReferenceType.YES_NO.getEntity().id())
    			.collect(Collectors.toList())));
	}
    
    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }
}

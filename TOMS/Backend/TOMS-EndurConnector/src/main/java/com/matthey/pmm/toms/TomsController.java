package com.matthey.pmm.toms;


import java.util.List;

import org.joda.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.PartyService;
import com.matthey.pmm.toms.service.UserService;
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
    
    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }
}

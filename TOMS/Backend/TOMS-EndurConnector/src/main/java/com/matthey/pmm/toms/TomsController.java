package com.matthey.pmm.toms;


import java.util.List;

import org.joda.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.PartyService;
import com.matthey.pmm.toms.transport.PartyTo;
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
    @GetMapping("parties")
    public List<PartyTo> retrieveEndurPartyList (List<PartyTo> knownParties) {
    	PartyService service = new PartyService (session);
    	return service.createToListDifference(knownParties);
    }
    
    
    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }
}

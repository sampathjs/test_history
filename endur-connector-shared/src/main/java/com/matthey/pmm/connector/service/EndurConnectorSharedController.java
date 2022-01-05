package com.matthey.pmm.connector.service;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.tradebooking.app.TradeBookingMain;
import com.matthey.pmm.tradebooking.processors.FileProcessor;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.openlink.util.constrepository.ConstRepository;

import ch.qos.logback.classic.Logger;

@RestController
@RequestMapping("/shared")
public class EndurConnectorSharedController {
    private static final Logger logger = EndurLoggerFactory.getLogger(TradeBookingMain.class);

	private static final String CONTEXT = "EndurConnectorShared";

	private static final String SUBCONTEXT = "TradeBooking";
    
    private final Session session;

    public EndurConnectorSharedController(Session session) {
        this.session = session;
    }
    
    @PostMapping("tradeBooking")
    public String postEmailConfirmationAction(@RequestParam String clientName,
    		@RequestBody String tradeBookingActionPlan) {
        ConstRepository constRepo = initConstRepo(CONTEXT, SUBCONTEXT); 
    	FileProcessor fp = new FileProcessor(session, constRepo, logger);
    	return "Test";
    }

	private ConstRepository initConstRepo(String context, String subContext){
		ConstRepository constRepo;
		try {
			constRepo = new ConstRepository(context, subContext);
			return constRepo;
		} catch (OException e) {
			throw new RuntimeException ("Error initialising ConstRepository");
		}
	}
}

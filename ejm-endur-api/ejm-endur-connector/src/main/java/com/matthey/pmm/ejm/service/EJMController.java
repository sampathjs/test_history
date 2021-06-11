package com.matthey.pmm.ejm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.ejm.Account;
import com.matthey.pmm.ejm.AccountBalance;
import com.matthey.pmm.ejm.BSTransaction;
import com.matthey.pmm.ejm.DTRTransaction;
import com.matthey.pmm.ejm.DailyAccountBalance;
import com.matthey.pmm.ejm.EmailConfirmationAction;
import com.matthey.pmm.ejm.GenericAction;
import com.matthey.pmm.ejm.ServiceUser;
import com.matthey.pmm.ejm.Specification;
import com.matthey.pmm.ejm.SpecificationSummary;
import com.matthey.pmm.ejm.Statement;
import com.matthey.pmm.ejm.Transaction;
import com.matthey.pmm.ejm.data.AccountBalancesRetriever;
import com.matthey.pmm.ejm.data.AccountRetriever;
import com.matthey.pmm.ejm.data.BSTransactionRetriever;
import com.matthey.pmm.ejm.data.DTRTransactionRetriever;
import com.matthey.pmm.ejm.data.DailyAccountBalancesRetriever;
import com.matthey.pmm.ejm.data.EmailConfirmationActionProcessor;
import com.matthey.pmm.ejm.data.GenericActionRetriever;
import com.matthey.pmm.ejm.data.ServiceAccountRetriever;
import com.matthey.pmm.ejm.data.SpecificationRetriever;
import com.matthey.pmm.ejm.data.SpecificationSummaryRetriever;
import com.matthey.pmm.ejm.data.StatementsRetriever;
import com.matthey.pmm.ejm.data.TransactionsRetriever;
import com.olf.openrisk.application.Session;

@RestController
@RequestMapping("/ejm")
public class EJMController {
	
    private static final Logger logger = LogManager.getLogger(EJMController.class);


    private final Session session;

    public EJMController(Session session) {
        this.session = session;
    }

    @GetMapping("/account_balances")
    public Set<AccountBalance> getAccountBalances(@RequestParam String account, @RequestParam String date) {
        return new AccountBalancesRetriever(session).retrieve(date, account);
    }

    @GetMapping("/accounts")
    public Set<Account> getAccount(@RequestParam String account) {
        return new AccountRetriever(session).retrieve(account);
    }

    @GetMapping("/statements")
    public Set<Statement> getStatement(@RequestParam String account,
                                       @RequestParam int year,
                                       @RequestParam String month,
                                       @RequestParam String type) {
        return new StatementsRetriever(session).retrieve(account, year, month, type);
    }

    @GetMapping("/specifications")
    public Set<Specification> getSpecifications(@RequestParam int tradeRef, @RequestParam String type) {
        return new SpecificationRetriever(session).retrieve(tradeRef, type);
    }

    @GetMapping("/events/daily_summary")
    public Set<DailyAccountBalance> getDailyAccountBalances(@RequestParam String account,
                                                            @RequestParam String metal,
                                                            @RequestParam String startDate,
                                                            @RequestParam String endDate) {
        return new DailyAccountBalancesRetriever(session).retrieve(account, metal, startDate, endDate);
    }

    @GetMapping("/events/BS")
    public Set<BSTransaction> getEventsForBS(@RequestParam String account, @RequestParam int tradeRef) {
        return new BSTransactionRetriever(session).retrieve(account, tradeRef);
    }

    @GetMapping("/events/DTR")
    public Set<DTRTransaction> getEventsForDTR(@RequestParam String account, @RequestParam int tradeRef) {
        return new DTRTransactionRetriever(session).retrieve(account, tradeRef);
    }

    @GetMapping("/events")
    public Set<Transaction> getEvents(@RequestParam String account,
                                      @RequestParam String metal,
                                      @RequestParam String startDate,
                                      @RequestParam String endDate) {
        return new TransactionsRetriever(session).retrieve(account, metal, startDate, endDate);
    }

    @GetMapping("/events/specifications")
    public Set<SpecificationSummary> getEventsForSpec(@RequestParam String account,
                                                      @RequestParam String metal,
                                                      @RequestParam String startDate,
                                                      @RequestParam String endDate) {
        return new SpecificationSummaryRetriever(session).retrieve(account, metal, startDate, endDate);
    }

    @GetMapping("/users")
    public Set<ServiceUser> getServiceUsers() {
        return new ServiceAccountRetriever(session).retrieve();
    }
    
    @GetMapping("/generic_action")
    public Set<GenericAction> getGenericAction(@RequestParam String actionId) {
    	logger.info("Retrieving generic action");
        return new GenericActionRetriever(session).retrieve(actionId);
    }
        
    
    @PostMapping("emailConfirmation/response")
    public String postEmailConfirmationAction(@RequestParam String actionId) {
    	List<EmailConfirmationAction> details = new ArrayList<>(new EmailConfirmationActionProcessor(session).retrieve(actionId));
    	EmailConfirmationActionProcessor ecap = new EmailConfirmationActionProcessor(session);
    	if (details != null && details.size() == 1) {
    		String status = details.get(0).emailStatus();
    		if (!status.equals("Open")) {
    			logger.warn("The document #" + details.get(0).documentId() + " has already been progressed"
    					+ " to status '" + status + "'");
    			return "Error: The document has already progressed to status '" + status + "'";
    		}
    		if (!ecap.checkDocumentExists(details.get(0).documentId()) ) {
    			logger.warn("The document #" + details.get(0).documentId() + " does no longer exist in the Endur core tables");
    			return "Error: The provided link is not valid (any longer)";
    		}
    		boolean isDispute = details.get(0).actionIdDispute().equals(actionId);
    		boolean isConfirm = details.get(0).actionIdConfirm().equals(actionId);
    		if (isDispute) {
    			ecap.patchEmailConfirmationAction(actionId, "Disputed");
    		} else if (isConfirm) {
    			ecap.patchEmailConfirmationAction(actionId, "Confirmed");
    		}
    	} else if (details == null || details.size() == 0) {
    		return "Error: The provided link is not valid (any longer)";
    	} else { // more than one result
    		return "Error: An internal error has occured. Please contact the JM support";
    	}
    	return "The document has been processed to the new status";
    }
}

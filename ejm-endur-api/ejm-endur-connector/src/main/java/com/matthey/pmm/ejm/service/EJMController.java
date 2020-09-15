package com.matthey.pmm.ejm.service;

import com.matthey.pmm.ejm.Account;
import com.matthey.pmm.ejm.AccountBalance;
import com.matthey.pmm.ejm.BSTransaction;
import com.matthey.pmm.ejm.DTRTransaction;
import com.matthey.pmm.ejm.DailyAccountBalance;
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
import com.matthey.pmm.ejm.data.ServiceAccountRetriever;
import com.matthey.pmm.ejm.data.SpecificationRetriever;
import com.matthey.pmm.ejm.data.SpecificationSummaryRetriever;
import com.matthey.pmm.ejm.data.StatementsRetriever;
import com.matthey.pmm.ejm.data.TransactionsRetriever;
import com.olf.openrisk.application.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/ejm")
public class EJMController {

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
}

package com.matthey.pmm.metal.transfers.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.metal.transfers.Account;
import com.matthey.pmm.metal.transfers.CashDeal;
import com.matthey.pmm.metal.transfers.CashDealBookingRun;
import com.matthey.pmm.metal.transfers.ClosingPrice;
import com.matthey.pmm.metal.transfers.DailyBalance;
import com.matthey.pmm.metal.transfers.InterestRate;
import com.matthey.pmm.metal.transfers.Party;
import com.matthey.pmm.metal.transfers.PartyContact;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.StatementEmailingRun;
import com.matthey.pmm.metal.transfers.StatementGeneratingRun;
import com.matthey.pmm.metal.transfers.WebsiteUser;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AbstractEndurConnectorController {

    @GetMapping("/current_date")
    String getCurrentDate();

    @GetMapping("/interest_rates/{indexName}")
    Set<InterestRate> getInterestRates(@PathVariable String indexName);

    @PutMapping("/interest_rates/{indexName}")
    void updateInterestRates(@RequestParam String user,
                             @PathVariable String indexName,
                             @RequestBody Set<InterestRate> interestRates);

    @GetMapping("/closing_prices/{indexName}/{refSource}")
    Set<ClosingPrice> getClosingPrices(@PathVariable String indexName,
                                       @PathVariable String refSource,
                                       @RequestParam String startDate,
                                       @RequestParam String endDate);

    @GetMapping("/accounts")
    Set<Account> getAccounts();

    @GetMapping("/account_balances/{date}")
    Set<DailyBalance> getAccountBalances(@PathVariable String date);

    @GetMapping("/parties")
    Set<Party> getParties();

    @GetMapping("/party_contacts")
    Set<PartyContact> getPartyContacts();

    @GetMapping("/fee_portfolios")
    Map<String, String> getFeePortfolios();

    @GetMapping("/cash_deals")
    List<CashDeal> getCashDeals();

    @GetMapping("/cash_deals/booking_status")
    boolean isDealBookingRunning();

    @PostMapping("/cash_deals")
    void bookCashDeals(@RequestBody List<CashDeal> deals);

    @GetMapping("/invoices/complete_status")
    boolean allInvoicesGenerated();

    @GetMapping("/invoices/generating_status/{region}")
    boolean isInvoiceGeneratingRunning(@PathVariable Region region);

    @PostMapping("/invoices/{region}")
    void generateInvoices(@PathVariable Region region);

    @GetMapping("/support_emails")
    Set<String> getSupportEmails();

    @GetMapping("/users")
    Set<WebsiteUser> getWebsiteUsers();

    @PostMapping("/users")
    void updateUser(@RequestBody WebsiteUser user);

    @GetMapping("/runs/statement_generating")
    List<StatementGeneratingRun> getStatementRuns();

    @PostMapping("/runs/statement_generating")
    void addStatementRuns(@RequestBody List<StatementGeneratingRun> runs);

    @GetMapping("/runs/statement_emailing")
    List<StatementEmailingRun> getEmailingRuns();

    @PostMapping("/runs/statement_emailing")
    void addEmailingRuns(@RequestBody List<StatementEmailingRun> runs);

    @GetMapping("/runs/cash_deal_booking")
    List<CashDealBookingRun> getCashDealBookingRuns();

    @PostMapping("/runs/cash_deal_booking")
    void addCashDealBookingRuns(@RequestBody List<CashDealBookingRun> runs);
}

package com.matthey.pmm.metal.transfers.service;

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
import com.matthey.pmm.metal.transfers.data.AccountBalancesRetriever;
import com.matthey.pmm.metal.transfers.data.AccountsRetriever;
import com.matthey.pmm.metal.transfers.data.CashDealProcessor;
import com.matthey.pmm.metal.transfers.data.ClosingPricesRetriever;
import com.matthey.pmm.metal.transfers.data.FeePortfoliosRetriever;
import com.matthey.pmm.metal.transfers.data.InterestRatesProcessor;
import com.matthey.pmm.metal.transfers.data.InvoiceProcessor;
import com.matthey.pmm.metal.transfers.data.PartiesRetriever;
import com.matthey.pmm.metal.transfers.data.PartyContactsRetriever;
import com.matthey.pmm.metal.transfers.data.SupportUserRetriever;
import com.matthey.pmm.metal.transfers.data.WebsiteUsersUpdater;
import com.matthey.pmm.metal.transfers.results.CashDealBookingRunProcessor;
import com.matthey.pmm.metal.transfers.results.StatementEmailingRunProcessor;
import com.matthey.pmm.metal.transfers.results.StatementGeneratingRunProcessor;
import com.olf.openrisk.application.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("MVCPathVariableInspection") // the paths are actually defined in AbstractEndurConnectorController
@RestController
public class MetalRentalsController implements AbstractEndurConnectorController {

    private static final Logger logger = LogManager.getLogger(MetalRentalsController.class);

    private final Session session;

    public MetalRentalsController(Session session) {
        this.session = session;
    }

    @Override
    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }

    @Override
    public Set<InterestRate> getInterestRates(@PathVariable String indexName) {
        logger.info("getInterestRates with indexName = {}", indexName);
        return new InterestRatesProcessor(session).retrieve(indexName);
    }

    @Override
    public void updateInterestRates(@RequestParam String user,
                                    @PathVariable String indexName,
                                    @RequestBody Set<InterestRate> interestRates) {
        logger.info("updateInterestRates with indexName = {} and rates = {}", indexName, interestRates);
        new InterestRatesProcessor(session).recordRequest(user, indexName, interestRates);
        session.getControlFactory().runTask("Metal Rentals Interest Rate Updater");
    }

    @Override
    public Set<ClosingPrice> getClosingPrices(@PathVariable String indexName,
                                              @PathVariable String refSource,
                                              @RequestParam String startDate,
                                              @RequestParam String endDate) {
        logger.info(
                "getClosingDatasetForIndex started with indexName = {}, refSource = {}, startDate = {}, endDate = {}",
                indexName,
                refSource,
                startDate,
                endDate);
        return new ClosingPricesRetriever(session).retrieve(indexName, refSource, startDate, endDate);
    }

    @Override
    public Set<Account> getAccounts() {
        return new AccountsRetriever(session).retrieve();
    }

    @Override
    public Set<DailyBalance> getAccountBalances(@PathVariable String date) {
        logger.info("getDailyBalances with date = {}", date);
        return new AccountBalancesRetriever(session).retrieve(date);
    }

    @Override
    public Set<Party> getParties() {
        return new PartiesRetriever(session).retrieve();
    }

    @Override
    public Set<PartyContact> getPartyContacts() {
        return new PartyContactsRetriever(session).retrieve();
    }

    @Override
    public Map<String, String> getFeePortfolios() {
        return new FeePortfoliosRetriever(session).retrieve();
    }

    @Override
    public List<CashDeal> getCashDeals() {
        return new CashDealProcessor(session).getCashDeals();
    }

    @Override
    public boolean isDealBookingRunning() {
        return new CashDealProcessor(session).isDealBookingRunning();
    }

    @Override
    public void bookCashDeals(@RequestBody List<CashDeal> deals) {
        new CashDealProcessor(session).startDealBooking(deals);
    }

    @Override
    public boolean allInvoicesGenerated() {
        CashDealProcessor cashDealProcessor = new CashDealProcessor(session);
        CashDealBookingRunProcessor cashDealBookingRunProcessor = new CashDealBookingRunProcessor(session);
        InvoiceProcessor invoiceProcessor = new InvoiceProcessor(session,
                                                                 cashDealProcessor,
                                                                 cashDealBookingRunProcessor);
        return invoiceProcessor.retrieveUndesignatedEvents().isEmpty() &&
               invoiceProcessor.retrieveExistingDocuments().getRowCount() == 0;
    }

    @Override
    public boolean isInvoiceGeneratingRunning(@PathVariable Region region) {
        CashDealProcessor cashDealProcessor = new CashDealProcessor(session);
        CashDealBookingRunProcessor cashDealBookingRunProcessor = new CashDealBookingRunProcessor(session);
        InvoiceProcessor invoiceProcessor = new InvoiceProcessor(session,
                                                                 cashDealProcessor,
                                                                 cashDealBookingRunProcessor);
        return invoiceProcessor.isInvoiceGeneratingRunning(region);
    }

    @Override
    public void generateInvoices(@PathVariable Region region) {
        CashDealProcessor cashDealProcessor = new CashDealProcessor(session);
        CashDealBookingRunProcessor cashDealBookingRunProcessor = new CashDealBookingRunProcessor(session);
        InvoiceProcessor invoiceProcessor = new InvoiceProcessor(session,
                                                                 cashDealProcessor,
                                                                 cashDealBookingRunProcessor);
        invoiceProcessor.startInvoiceGenerating(region);
    }

    @Override
    public Set<String> getSupportEmails() {
        return new SupportUserRetriever(session).retrieve();
    }

    @Override
    public Set<WebsiteUser> getWebsiteUsers() {
        return new WebsiteUsersUpdater(session).retrieve();
    }

    @Override
    public void updateUser(@RequestBody WebsiteUser user) {
        new WebsiteUsersUpdater(session).update(user);
    }

    @Override
    public List<StatementGeneratingRun> getStatementRuns() {
        return new StatementGeneratingRunProcessor(session).retrieveLatest();
    }

    @Override
    public void addStatementRuns(@RequestBody List<StatementGeneratingRun> runs) {
        new StatementGeneratingRunProcessor(session).add(runs);
    }

    @Override
    public List<StatementEmailingRun> getEmailingRuns() {
        return new StatementEmailingRunProcessor(session).retrieveLatest();
    }

    @Override
    public void addEmailingRuns(@RequestBody List<StatementEmailingRun> runs) {
        new StatementEmailingRunProcessor(session).add(runs);
    }

    @Override
    public List<CashDealBookingRun> getCashDealBookingRuns() {
        return new CashDealBookingRunProcessor(session).retrieveLatest();
    }

    @Override
    public void addCashDealBookingRuns(@RequestBody List<CashDealBookingRun> runs) {
        new CashDealBookingRunProcessor(session).add(runs);
    }
}

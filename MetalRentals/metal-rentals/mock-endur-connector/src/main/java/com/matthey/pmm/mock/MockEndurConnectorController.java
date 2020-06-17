package com.matthey.pmm.mock;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.matthey.pmm.metal.rentals.Account;
import com.matthey.pmm.metal.rentals.CashDeal;
import com.matthey.pmm.metal.rentals.CashDealBookingRun;
import com.matthey.pmm.metal.rentals.ClosingPrice;
import com.matthey.pmm.metal.rentals.DailyBalance;
import com.matthey.pmm.metal.rentals.ImmutableAccount;
import com.matthey.pmm.metal.rentals.ImmutableClosingPrice;
import com.matthey.pmm.metal.rentals.ImmutableDailyBalance;
import com.matthey.pmm.metal.rentals.ImmutableInterestRate;
import com.matthey.pmm.metal.rentals.ImmutableParty;
import com.matthey.pmm.metal.rentals.ImmutablePartyContact;
import com.matthey.pmm.metal.rentals.ImmutableWebsiteUser;
import com.matthey.pmm.metal.rentals.InterestRate;
import com.matthey.pmm.metal.rentals.Party;
import com.matthey.pmm.metal.rentals.PartyContact;
import com.matthey.pmm.metal.rentals.Region;
import com.matthey.pmm.metal.rentals.StatementEmailingRun;
import com.matthey.pmm.metal.rentals.StatementGeneratingRun;
import com.matthey.pmm.metal.rentals.WebsiteUser;
import com.matthey.pmm.metal.rentals.service.AbstractEndurConnectorController;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.stream.Collectors.toSet;

@RestController
public class MockEndurConnectorController implements AbstractEndurConnectorController {

    private static final List<String> metals = List.of("XPT", "XPD", "XRH", "XIR", "XOS", "XRU", "XAU", "XAG");
    private static final List<String> partyNames = List.of("Comp1",
                                                           "Comp2",
                                                           "Comp3",
                                                           "JM PMM HK",
                                                           "JM PMM UK",
                                                           "JM PMM US");
    private final Map<String, Set<InterestRate>> interestRates = Maps.newHashMap();
    private final Map<IndexClosingDatasetSelector, Set<ClosingPrice>> closingPrices = Maps.newConcurrentMap();
    private final Set<Account> accounts = Sets.newHashSet();
    private final Map<String, Set<DailyBalance>> dailyBalances = Maps.newConcurrentMap();
    private final Set<Party> parties = Sets.newHashSet();
    private final Map<String, String> feePortfolios = Maps.newHashMap();
    private final Set<PartyContact> partyContacts = Sets.newHashSet();
    private String currentDate = null;

    public MockEndurConnectorController() {
        setInitialInterestRates();
        setInitialClosingPrices();
        setInitialAccounts();
        setInitialDailyBalances();
        setInitialParties();
        setInitialPartyContacts();
        setInitialPortfolios();
        setInitialCurrentDate();
    }

    private static String getRefSource(String metal, boolean isForCN) {
        switch (metal) {
            case "XAU":
                return "LBMA PM";
            case "XAG":
                return "LBMA Silver";
            default:
                return isForCN ? "JM HK Opening" : "JM London Opening";
        }
    }

    private void setInitialInterestRates() {
        interestRates.put("JM_Metals_Util_Rates",
                          Set.of(ImmutableInterestRate.of("XPT", 0.0040),
                                 ImmutableInterestRate.of("XPD", 0.0050),
                                 ImmutableInterestRate.of("XRH", 0.0060),
                                 ImmutableInterestRate.of("XIR", 0.0070),
                                 ImmutableInterestRate.of("XOS", 0.0080),
                                 ImmutableInterestRate.of("XRU", 0.0090),
                                 ImmutableInterestRate.of("XAU", 0.01),
                                 ImmutableInterestRate.of("XAG", 0.011)));
        interestRates.put("JM_CN_Metal_Util_Rates",
                          Set.of(ImmutableInterestRate.of("XPT", 0.0050),
                                 ImmutableInterestRate.of("XPD", 0.0060),
                                 ImmutableInterestRate.of("XRH", 0.0070),
                                 ImmutableInterestRate.of("XIR", 0.0080),
                                 ImmutableInterestRate.of("XOS", 0.0090),
                                 ImmutableInterestRate.of("XRU", 0.01),
                                 ImmutableInterestRate.of("XAU", 0.011),
                                 ImmutableInterestRate.of("XAG", 0.012)));
    }

    private void setInitialClosingPrices() {
        var startDateForNonCN = LocalDate.of(2019, 12, 1);
        var endDateForNonCN = LocalDate.of(2020, 1, 1);
        var startDateForCN = LocalDate.of(2019, 12, 26);
        var endDateForCN = LocalDate.of(2020, 1, 26);
        Set<ClosingPrice> prices = startDateForNonCN.datesUntil(endDateForNonCN)
                .map(date -> ImmutableClosingPrice.of(ISO_LOCAL_DATE.format(date), 100.0))
                .collect(toSet());
        for (var metal : metals) {
            for (var currency : List.of("USD", "EUR", "GBP")) {
                closingPrices.put(ImmutableIndexClosingDatasetSelector.builder()
                                          .index(metal + "." + currency)
                                          .refSource(getRefSource(metal, false))
                                          .startDate(ISO_LOCAL_DATE.format(startDateForNonCN))
                                          .endDate(ISO_LOCAL_DATE.format(endDateForNonCN.minusDays(1)))
                                          .build(), prices);
            }
            closingPrices.put(ImmutableIndexClosingDatasetSelector.builder()
                                      .index(metal + ".USD")
                                      .refSource(getRefSource(metal, true))
                                      .startDate(ISO_LOCAL_DATE.format(startDateForCN))
                                      .endDate(ISO_LOCAL_DATE.format(endDateForCN.minusDays(1)))
                                      .build(), prices);
        }
        closingPrices.put(ImmutableIndexClosingDatasetSelector.builder()
                                  .index("FX_USD.CNY")
                                  .refSource("BOC")
                                  .startDate(ISO_LOCAL_DATE.format(startDateForCN))
                                  .endDate(ISO_LOCAL_DATE.format(endDateForCN.minusDays(1)))
                                  .build(), prices);
    }

    private void setInitialAccounts() {
        accounts.add(ImmutableAccount.builder()
                             .name("Comp1@Comp2")
                             .type("Vostro")
                             .reportingUnit("TOz")
                             .preferredCurrency("USD")
                             .owner("Comp1")
                             .holder("Comp2")
                             .build());
        accounts.add(ImmutableAccount.builder()
                             .name("Comp1@Comp2/ING")
                             .type("Vostro")
                             .reportingUnit("TOz")
                             .preferredCurrency("USD")
                             .owner("Comp1")
                             .holder("Comp2")
                             .build());
        accounts.add(ImmutableAccount.builder()
                             .name("Comp3@JM PMM HK")
                             .type("Vostro")
                             .reportingUnit("TOz")
                             .preferredCurrency("USD")
                             .owner("Comp3")
                             .holder("JM PMM HK")
                             .build());
        accounts.add(ImmutableAccount.builder()
                             .name("Comp4@JM PMM CN")
                             .type("Vostro")
                             .reportingUnit("TOz")
                             .preferredCurrency("CNY")
                             .owner("Comp4")
                             .holder("JM PMM CN")
                             .build());
        accounts.add(ImmutableAccount.builder()
                             .name("JM PMM UK@JM PMM US")
                             .type("Nostro")
                             .reportingUnit("TOz")
                             .preferredCurrency("GBP")
                             .owner("JM PMM UK")
                             .holder("JM PMM US")
                             .internalBorrowings("UK-US")
                             .build());
        accounts.add(ImmutableAccount.builder()
                             .name("JM PMM US@JM PMM UK")
                             .type("Nostro")
                             .reportingUnit("TOz")
                             .preferredCurrency("GBP")
                             .owner("JM PMM US")
                             .holder("JM PMM UK")
                             .internalBorrowings("UK-US")
                             .build());
    }

    private void setInitialDailyBalances() {
        var startDate = LocalDate.of(2019, 12, 1);
        var endDate = LocalDate.of(2020, 1, 26);

        for (var account : accounts) {
            startDate.datesUntil(endDate).forEach(date -> {
                var dateStr = ISO_LOCAL_DATE.format(date);
                Set<DailyBalance> balances = metals.stream()
                        .map(metal -> ImmutableDailyBalance.builder()
                                .account(account.name())
                                .date(dateStr)
                                .metal(metal)
                                .balance(1000.0)
                                .balanceInTOz(1000.0)
                                .build())
                        .collect(toSet());
                dailyBalances.put(dateStr, balances);
            });
        }
    }

    private void setInitialParties() {
        partyNames.forEach(name -> parties.add(ImmutableParty.builder()
                                                       .name(name)
                                                       .address("Address")
                                                       .telephone("0000000")
                                                       .vatNumber("GB111111")
                                                       .build()));
    }

    private void setInitialPartyContacts() {
        partyContacts.add(ImmutablePartyContact.builder()
                                  .party("JM PMM UK")
                                  .contact("contact1")
                                  .email("email1")
                                  .build());
        partyContacts.add(ImmutablePartyContact.builder()
                                  .party("JM PMM HK")
                                  .contact("contact2")
                                  .email("email2")
                                  .build());
        partyContacts.add(ImmutablePartyContact.builder()
                                  .party("JM PMM US")
                                  .contact("contact3")
                                  .email("email3")
                                  .build());
    }

    private void setInitialPortfolios() {
        partyNames.forEach(name -> feePortfolios.put(name, "Fees"));
    }

    private void setInitialCurrentDate() {
        this.currentDate = "2020-01-01";
    }

    @Override
    public String getCurrentDate() {
        return currentDate;
    }

    @Override
    public Set<InterestRate> getInterestRates(String indexName) {
        return interestRates.get(indexName);
    }

    @Override
    public void updateInterestRates(String user, String indexName, Set<InterestRate> interestRates) {
        this.interestRates.put(indexName, interestRates);
    }

    @Override
    public Set<ClosingPrice> getClosingPrices(String indexName, String refSource, String startDate, String endDate) {
        return closingPrices.get(ImmutableIndexClosingDatasetSelector.builder()
                                         .index(indexName)
                                         .refSource(refSource)
                                         .startDate(startDate)
                                         .endDate(endDate)
                                         .build());
    }

    @Override
    public Set<Account> getAccounts() {
        return accounts;
    }

    @Override
    public Set<DailyBalance> getAccountBalances(String date) {
        return dailyBalances.get(date);
    }

    @Override
    public Set<Party> getParties() {
        return this.parties;
    }

    @Override
    public Set<PartyContact> getPartyContacts() {
        return this.partyContacts;
    }

    @Override
    public Map<String, String> getFeePortfolios() {
        return feePortfolios;
    }

    @Override
    public List<CashDeal> getCashDeals() {
        return List.of();
    }

    @Override
    public boolean isDealBookingRunning() {
        return false;
    }

    @Override
    public void bookCashDeals(List<CashDeal> cashDeals) {
    }

    @Override
    public boolean allInvoicesGenerated() {
        return true;
    }

    @Override
    public boolean isInvoiceGeneratingRunning(Region region) {
        return false;
    }

    @Override
    public void generateInvoices(Region region) {
    }

    @Override
    public Set<String> getSupportEmails() {
        return Set.of();
    }

    @Override
    public Set<WebsiteUser> getWebsiteUsers() {
        return Set.of(ImmutableWebsiteUser.builder()
                              .id(0)
                              .userFullName("Test User")
                              .email("test.user@example.com")
                              .encryptedPassword(new BCryptPasswordEncoder().encode("testing_password"))
                              .build());
    }

    @Override
    public void updateUser(WebsiteUser user) {
    }

    @Override
    public List<StatementGeneratingRun> getStatementRuns() {
        return List.of();
    }

    @Override
    public void addStatementRuns(List<StatementGeneratingRun> runs) {
    }

    @Override
    public List<StatementEmailingRun> getEmailingRuns() {
        return List.of();
    }

    @Override
    public void addEmailingRuns(List<StatementEmailingRun> runs) {
    }

    @Override
    public List<CashDealBookingRun> getCashDealBookingRuns() {
        return List.of();
    }

    @Override
    public void addCashDealBookingRuns(List<CashDealBookingRun> runs) {
    }

    @PostMapping("/fee_portfolios")
    public void setFeePortfolios(Map<String, String> feePortfolios) {
        this.feePortfolios.clear();
        this.feePortfolios.putAll(feePortfolios);
    }

    @PostMapping
    public void setPartyContacts(Set<PartyContact> partyContacts) {
        this.partyContacts.clear();
        this.partyContacts.addAll(partyContacts);
    }

    @PostMapping("/parties")
    public void setParties(Set<Party> parties) {
        this.parties.clear();
        this.parties.addAll(parties);
    }

    @PostMapping("/accounts")
    public void setAccounts(@RequestBody Set<Account> accounts) {
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    @PutMapping("/current_date")
    public void setCurrentDate(@RequestBody String currentDate) {
        this.currentDate = currentDate;
    }

    @PostMapping("/closing_prices/{indexName}/{refSource}")
    public void setClosingDatasetForIndex(@PathVariable String indexName,
                                          @PathVariable String refSource,
                                          @RequestParam String startDate,
                                          @RequestParam String endDate,
                                          @RequestBody Set<ClosingPrice> dataset) {
        closingPrices.put(ImmutableIndexClosingDatasetSelector.builder()
                                  .index(indexName)
                                  .refSource(refSource)
                                  .startDate(startDate)
                                  .endDate(endDate)
                                  .build(), dataset);
    }

    @PostMapping("/account_balances/{date}")
    public void setDailyBalances(@PathVariable String date, @RequestBody Set<DailyBalance> dailyBalances) {
        this.dailyBalances.put(date, dailyBalances);
    }
}

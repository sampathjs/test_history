package com.matthey.pmm.metal.transfers.data;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.Party;
import com.matthey.pmm.metal.transfers.PartyContact;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.service.MetalRentalsController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.matthey.pmm.metal.transfers.PropertyChecker.checkAndReturn;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;


@Component
public class DataCache {

    private static final Logger logger = LoggerFactory.getLogger(MetalRentalsController.class);
    private static final int UPDATE_INTERVAL = 1000 * 60 * 30;

    private final AtomicReference<InterestRates> interestRates = new AtomicReference<>();
    private final AtomicReference<AveragePrices> averagePrices = new AtomicReference<>();
    private final AtomicReference<Accounts> accounts = new AtomicReference<>();
    private final AtomicReference<DailyBalances> dailyBalances = new AtomicReference<>();
    private final AtomicReference<Map<String, Party>> parties = new AtomicReference<>();
    private final AtomicReference<Map<String, Set<PartyContact>>> partyContacts = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> feePortfolios = new AtomicReference<>();
    private final AtomicReference<StatementPeriods> statementPeriods = new AtomicReference<>();
    private final EndurConnector endurConnector;
    private final List<String> metals;
    private final List<String> currencies;
    private final Map<String, String> holdingBanks;
    private final Map<Region, String> interestRatesIndexes;

    public DataCache(EndurConnector endurConnector,
                     @Value("${metal.list}") List<String> metals,
                     @Value("${currency.list}") List<String> currencies,
                     @Value("#{${holding.banks}}") Map<String, String> holdingBanks,
                     @Value("#{${interest.rates.indexes}}") Map<Region, String> interestRatesIndexes) {
        this.endurConnector = endurConnector;
        this.metals = checkAndReturn(metals, !metals.isEmpty(), "metals");
        this.currencies = checkAndReturn(currencies, !currencies.isEmpty(), "currencies");
        this.holdingBanks = checkAndReturn(holdingBanks, !holdingBanks.isEmpty(), "holding banks");
        this.interestRatesIndexes = checkAndReturn(interestRatesIndexes,
                                                   !interestRatesIndexes.isEmpty(),
                                                   "interest rates indexes");

        updateParties();
        updatePartyContacts();
        updatePortfolios();
        updateAccounts();
        updateInterestRates();
        updateStatementPeriods();
        updateAveragePrices();
        updateDailyBalances();
    }

    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL)
    private void updateParties() {
        updateData("parties", () -> {
            Party[] parties = endurConnector.get("/parties", Party[].class);
            this.parties.set(Maps.uniqueIndex(Arrays.asList(parties), Party::name));
        });
    }

    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL)
    private void updatePartyContacts() {
        updateData("party contacts", () -> {
            PartyContact[] partyContacts = endurConnector.get("/party_contacts", PartyContact[].class);
            this.partyContacts.set(Arrays.stream(partyContacts).collect(groupingBy(PartyContact::party, toSet())));
        });
    }

    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL)
    @SuppressWarnings("unchecked")
    private void updatePortfolios() {
        updateData("fee portfolios", () -> {
            Map<String, String> portfolios = endurConnector.get("/fee_portfolios", Map.class);
            this.feePortfolios.set(portfolios);
        });
    }

    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL)
    private void updateAccounts() {
        updateData("accounts", () -> accounts.set(new Accounts(endurConnector)));
    }

    @Scheduled(fixedDelay = 1000 * 30)
    private void updateInterestRates() {
        updateData("interest rates",
                   () -> interestRates.set(new InterestRates(endurConnector, metals, interestRatesIndexes)));
    }

    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL)
    private void updateStatementPeriods() {
        updateData("current date", () -> {
            String currentDate = endurConnector.get("/current_date", String.class);
            statementPeriods.set(new StatementPeriods(LocalDate.parse(currentDate, ISO_LOCAL_DATE)));
        });
    }

    // start late compared to the others so the dependencies can get ready
    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL + 1000 * 60)
    private void updateAveragePrices() {
        updateData("average prices",
                   () -> averagePrices.set(new AveragePrices(endurConnector,
                                                             metals,
                                                             currencies,
                                                             getStatementPeriods())));
    }

    // start late compared to the others so the dependencies can get ready
    @Scheduled(fixedDelay = UPDATE_INTERVAL, initialDelay = UPDATE_INTERVAL + 1000 * 60)
    private void updateDailyBalances() {
        updateData("daily balances",
                   () -> dailyBalances.set(new DailyBalances(endurConnector,
                                                             holdingBanks,
                                                             getAccounts().asMap(),
                                                             getStatementPeriods())));
    }

    private void updateData(String dataName, Runnable updater) {
        var stopwatch = Stopwatch.createStarted();
        try {
            updater.run();
        } catch (Exception e) {
            logger.error("an error occurred during updating {}: {}", dataName, e.getMessage(), e);
        } finally {
            stopwatch.stop();
            logger.info("finished updating {} within {} ms", dataName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public StatementPeriods getStatementPeriods() {
        return statementPeriods.get();
    }

    public Accounts getAccounts() {
        return accounts.get();
    }

    public InterestRates getInterestRates() {
        return interestRates.get();
    }

    public AveragePrices getAveragePrices() {
        return averagePrices.get();
    }

    public DailyBalances getDailyBalances() {
        return dailyBalances.get();
    }

    public Map<String, Party> getParties() {
        return parties.get();
    }

    public Map<String, Set<PartyContact>> getPartyContacts() {
        return partyContacts.get();
    }

    public Map<String, String> getFeePortfolios() {
        return feePortfolios.get();
    }
}

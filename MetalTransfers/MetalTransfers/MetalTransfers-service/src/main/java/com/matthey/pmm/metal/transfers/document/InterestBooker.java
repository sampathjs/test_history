package com.matthey.pmm.metal.transfers.document;

import com.google.common.collect.Maps;
import com.matthey.pmm.metal.transfers.CashDeal;
import com.matthey.pmm.metal.transfers.CashDealBookingRun;
import com.matthey.pmm.metal.transfers.CashDealKey;
import com.matthey.pmm.metal.transfers.EndurConnector;
import com.matthey.pmm.metal.transfers.ImmutableCashDealBookingRun;
import com.matthey.pmm.metal.transfers.ImmutableCashDealKey;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.RunResult;
import com.matthey.pmm.metal.transfers.interest.Interest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.matthey.pmm.metal.transfers.RunResult.Failed;
import static com.matthey.pmm.metal.transfers.RunResult.Skipped;
import static com.matthey.pmm.metal.transfers.RunResult.Successful;
import static java.util.stream.Collectors.toList;

@Component
public class InterestBooker {

    private static final Logger logger = LoggerFactory.getLogger(InterestBooker.class);

    private final EndurConnector endurConnector;
    private final CashDealGenerator cashDealGenerator;
    private final int workflowCheckInterval;

    public InterestBooker(EndurConnector endurConnector,
                          CashDealGenerator cashDealGenerator,
                          @Value("${workflow.check.interval.in.milliseconds}") int workflowCheckInterval) {
        this.endurConnector = endurConnector;
        this.cashDealGenerator = cashDealGenerator;
        this.workflowCheckInterval = workflowCheckInterval;
    }

    public List<CashDealBookingRun> book(List<Interest> interests,
                                         Region region,
                                         String statementMonth,
                                         LocalDate statementDate,
                                         String user) {
        var existingDeals = getExistingDeals();
        var deals = interests.stream()
                .map(interest -> cashDealGenerator.generate(interest, region, statementDate))
                .collect(toList());
        var newDeals = deals.stream().filter(deal -> !existingDeals.containsKey(getKey(deal))).collect(toList());
        endurConnector.post("/cash_deals", newDeals);
        waitForWorkFinished("/cash_deals/booking_status");
        return verifyDealBooking(deals, existingDeals, statementMonth, user);
    }

    private Map<CashDealKey, CashDeal> getExistingDeals() {
        return Maps.uniqueIndex(List.of(endurConnector.get("/cash_deals", CashDeal[].class)), this::getKey);
    }

    private List<CashDealBookingRun> verifyDealBooking(List<CashDeal> deals,
                                                       Map<CashDealKey, CashDeal> existingDeals,
                                                       String statementMonth,
                                                       String user) {
        var bookedDeals = getExistingDeals();
        var runTime = LocalDateTime.now(ZoneOffset.UTC).toString();
        List<CashDealBookingRun> cashDealBookingRuns = deals.stream()
                .map(deal -> ImmutableCashDealBookingRun.builder()
                        .user(user)
                        .runTime(runTime)
                        .statementMonth(statementMonth)
                        .result(getResult(deal, existingDeals.keySet(), bookedDeals.keySet()))
                        .deal(bookedDeals.getOrDefault(getKey(deal), deal))
                        .build())
                .collect(Collectors.toList());
        endurConnector.saveRuns("/runs/cash_deal_booking?user={user}", cashDealBookingRuns, user);
        return cashDealBookingRuns;
    }

    private RunResult getResult(CashDeal deal, Set<CashDealKey> existingDealKeys, Set<CashDealKey> bookedDealKeys) {
        var key = getKey(deal);
        return existingDealKeys.contains(key) ? Skipped : (bookedDealKeys.contains(key) ? Successful : Failed);
    }

    public boolean generateInvoices(Region region) {
        try {
            endurConnector.post("/invoices/" + region.name(), null);
            waitForWorkFinished("/invoices/generating_status/" + region.name());
            return endurConnector.get("/invoices/complete_status", Boolean.class);
        } catch (Exception e) {
            logger.error("error occurred during invoice generating: {}", e.getMessage(), e);
            return false;
        }
    }

    private void waitForWorkFinished(String statusUrl) {
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(workflowCheckInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        while (endurConnector.get(statusUrl, Boolean.class));
    }

    private CashDealKey getKey(CashDeal cashDeal) {
        return ImmutableCashDealKey.builder()
                .cashflowType(cashDeal.cashflowType())
                .internalBU(cashDeal.internalBU())
                .externalBU(cashDeal.externalBU())
                .settleDate(cashDeal.settleDate())
                .position(new DecimalFormat("####0.00").format(cashDeal.position()))
                .build();
    }
}

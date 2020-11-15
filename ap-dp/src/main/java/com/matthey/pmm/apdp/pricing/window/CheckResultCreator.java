package com.matthey.pmm.apdp.pricing.window;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;
import static java.time.temporal.ChronoUnit.DAYS;

public class CheckResultCreator {
    
    private final LocalDate currentDate;
    private final Function<Integer, String> customerNameGetter;
    private final Map<PricingWindowKey, Integer> pricingWindows;
    
    public CheckResultCreator(LocalDate currentDate,
                              Function<Integer, String> customerNameGetter,
                              Map<PricingWindowKey, Integer> pricingWindows) {
        this.currentDate = currentDate;
        this.customerNameGetter = customerNameGetter;
        this.pricingWindows = pricingWindows;
    }
    
    public CheckResult from(UnmatchedDeal deal) {
        PricingWindowKey pricingWindowKey = deal.pricingWindowKey();
        checkState(pricingWindows.containsKey(pricingWindowKey), "invalid pricing window key: " + pricingWindowKey);
        LocalDate expiryDate = deal.dealDate().plusDays(pricingWindows.get(pricingWindowKey));
        
        return ImmutableCheckResult.builder()
                .pricingType(deal.pricingWindowKey().pricingType())
                .dealNum(Integer.toString(deal.dealNum()))
                .customer(customerNameGetter.apply(deal.pricingWindowKey().customerId()))
                .dealDate(deal.dealDate().toString())
                .expiryDate(expiryDate.toString())
                .numOfDaysToExpiry(Math.max(Math.toIntExact(DAYS.between(currentDate, expiryDate)), 0))
                .unmatchedVolume(deal.unmatchedVolume())
                .build();
    }
    
    public boolean needAlert(CheckResult checkResult) {
        return checkResult.numOfDaysToExpiry() <= 3;
    }
}

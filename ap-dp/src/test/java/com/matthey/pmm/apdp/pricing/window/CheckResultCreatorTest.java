package com.matthey.pmm.apdp.pricing.window;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CheckResultCreatorTest {
    
    private final LocalDate currentDate = LocalDate.of(2020, 10, 15);
    private final PricingWindowKey defaultPricingWindowKey = ImmutablePricingWindowKey.of(0, "AP", 0);
    private final Map<PricingWindowKey, Integer> defaultPricingWindows = ImmutableMap.of(defaultPricingWindowKey, 5);
    private final Function<Integer, String> defaultCustomerNameGetter = id -> "Test";
    
    @Test
    public void deal_is_not_expired() {
        UnmatchedDeal deal = ImmutableUnmatchedDeal.builder()
                .dealNum(11)
                .pricingWindowKey(defaultPricingWindowKey)
                .dealDate(LocalDate.of(2020, 10, 11))
                .build();
        CheckResultCreator sut = new CheckResultCreator(currentDate, id -> "Test 0", defaultPricingWindows);
        CheckResult expected = ImmutableCheckResult.builder()
                .pricingType("AP")
                .dealNum("11")
                .customer("Test 0")
                .dealDate("2020-10-11")
                .expiryDate("2020-10-16")
                .numOfDaysToExpiry(1)
                .build();
        assertThat(sut.from(deal)).isEqualTo(expected);
    }
    
    @Test
    public void deal_is_expired() {
        UnmatchedDeal deal = ImmutableUnmatchedDeal.builder()
                .dealNum(22)
                .pricingWindowKey(defaultPricingWindowKey)
                .dealDate(LocalDate.of(2020, 10, 9))
                .build();
        CheckResultCreator sut = new CheckResultCreator(currentDate, id -> "Test 1", defaultPricingWindows);
        CheckResult expected = ImmutableCheckResult.builder()
                .pricingType("AP")
                .dealNum("22")
                .customer("Test 1")
                .dealDate("2020-10-09")
                .expiryDate("2020-10-14")
                .numOfDaysToExpiry(0)
                .build();
        
        assertThat(sut.from(deal)).isEqualTo(expected);
    }
    
    @Test
    public void pricing_windows_are_pick_up_correctly() {
        PricingWindowKey key1 = ImmutablePricingWindowKey.of(0, "AP", 0);
        PricingWindowKey key2 = ImmutablePricingWindowKey.of(1, "AP", 0);
        PricingWindowKey key3 = ImmutablePricingWindowKey.of(0, "DP", 0);
        PricingWindowKey key4 = ImmutablePricingWindowKey.of(0, "AP", 1);
        Map<PricingWindowKey, Integer> pricingWindows = ImmutableMap.of(key1, 5, key2, 6, key3, 7, key4, 8);
        CheckResultCreator sut = new CheckResultCreator(currentDate, defaultCustomerNameGetter, pricingWindows);
        UnmatchedDeal deal1 = createDeal(key1, currentDate);
        assertThat(sut.from(deal1).expiryDate()).isEqualTo("2020-10-20");
        UnmatchedDeal deal2 = createDeal(key2, currentDate);
        assertThat(sut.from(deal2).expiryDate()).isEqualTo("2020-10-21");
        UnmatchedDeal deal3 = createDeal(key3, currentDate);
        assertThat(sut.from(deal3).expiryDate()).isEqualTo("2020-10-22");
        UnmatchedDeal deal4 = createDeal(key4, currentDate);
        assertThat(sut.from(deal4).expiryDate()).isEqualTo("2020-10-23");
    }
    
    private UnmatchedDeal createDeal(PricingWindowKey key, LocalDate dealDate) {
        return ImmutableUnmatchedDeal.builder().dealNum(0).pricingWindowKey(key).dealDate(dealDate).build();
    }
    
    @Test
    public void alert_is_not_needed() {
        UnmatchedDeal deal = createDeal(defaultPricingWindowKey, LocalDate.of(2020, 10, 14));
        CheckResultCreator sut = new CheckResultCreator(currentDate, defaultCustomerNameGetter, defaultPricingWindows);
        CheckResult result = sut.from(deal);
        assertThat(sut.needAlert(result)).isFalse();
    }
    
    @Test
    public void alert_is_needed() {
        UnmatchedDeal deal = createDeal(defaultPricingWindowKey, LocalDate.of(2020, 10, 12));
        CheckResultCreator sut = new CheckResultCreator(currentDate, defaultCustomerNameGetter, defaultPricingWindows);
        CheckResult result = sut.from(deal);
        assertThat(sut.needAlert(result)).isTrue();
    }
}
package com.matthey.pmm.metal.rentals.interest;

import com.google.common.collect.Lists;
import com.matthey.pmm.metal.rentals.Region;
import com.matthey.pmm.metal.rentals.data.EnhancedDailyBalance;
import com.matthey.pmm.metal.rentals.data.InternalBorrowingsGroup;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

@Component
public class InterestCalculator {

    private static final Logger logger = LoggerFactory.getLogger(InterestCalculator.class);

    private final Map<String, String> holdingBanks;
    private final List<String> cnZeroVatBUList;
    private final double cnVatRate;

    public InterestCalculator(@Value("#{${holding.banks}}") Map<String, String> holdingBanks,
                              @Value("${cn.zero.vat.bu.list}") List<String> cnZeroVatBUList,
                              @Value("${cn.vat.rate}") double cnVatRate) {
        this.holdingBanks = holdingBanks;
        this.cnZeroVatBUList = cnZeroVatBUList;
        this.cnVatRate = cnVatRate;
    }

    public Map<String, List<Interest>> calculateAllInterests(List<EnhancedDailyBalance> dailyBalances,
                                                             InterestCalculationParameters parameters,
                                                             int numOfDays,
                                                             Region region) {
        var accountGroups = parameters.accounts().keySet();
        var groupedDailyBalances = dailyBalances.stream()
                .filter(enhancedDailyBalance -> accountGroups.contains(enhancedDailyBalance.account().group()))
                .collect(groupingBy(EnhancedDailyBalance::accountGroup,
                                    groupingBy(EnhancedDailyBalance::interestKey,
                                               groupingBy(EnhancedDailyBalance::metal))));
        List<Interest> interests = Lists.newArrayList();
        for (var group : groupedDailyBalances.keySet()) {
            var balancesByGroup = groupedDailyBalances.get(group);
            for (var interestKey : balancesByGroup.keySet()) {
                var balancesByAccount = balancesByGroup.get(interestKey);
                for (var metal : balancesByAccount.keySet()) {
                    try {
                        var interest = getInterest(balancesByAccount.get(metal),
                                                   new GroupingKeys(group, interestKey, metal),
                                                   parameters,
                                                   numOfDays,
                                                   region);
                        interests.add(interest);
                    } catch (Exception e) {
                        logger.error("error occurred when generating interest:" + e.getMessage(), e);
                    }
                }
            }
        }
        return interests.stream().collect(groupingBy(Interest::group));
    }

    private Interest getInterest(List<EnhancedDailyBalance> dailyBalances,
                                 GroupingKeys keys,
                                 InterestCalculationParameters parameters,
                                 int numOfDays,
                                 Region region) {
        var internalBorrowingsGroup = InternalBorrowingsGroup.from(keys.group);
        String currency;
        String unit;
        String owner;
        String holder;
        if (internalBorrowingsGroup.isEmpty()) {
            var account = parameters.accountMap().get(keys.interestKey);
            var preferredCurrency = account.preferredCurrency();
            currency = StringUtils.isEmpty(preferredCurrency) ? "USD" : preferredCurrency;
            unit = account.reportingUnit();
            owner = account.owner();
            holder = account.holder();
        } else {
            currency = "USD";
            unit = "TOz";
            owner = holdingBanks.get(internalBorrowingsGroup.get().ownerRegion());
            holder = holdingBanks.get(internalBorrowingsGroup.get().holderRegion());
        }

        var interestRateWithVat = parameters.interestRates().get(keys.metal);
        var interestRate = cnZeroVatBUList.contains(owner)
                           ? interestRateWithVat / (1 + cnVatRate)
                           : interestRateWithVat;
        var averagePrice = parameters.averagePrices().get(currency).get(keys.metal) *
                           (region == Region.NonCN || cnZeroVatBUList.contains(owner) ? 1 : (1 + cnVatRate));
        var interest = ImmutableInterest.builder()
                .group(keys.group)
                .account(keys.interestKey)
                .metal(keys.metal)
                .unit(unit)
                .currency(currency)
                .averageBalanceInTOz(average(dailyBalances, EnhancedDailyBalance::balanceInTOz))
                .averageBalance(average(dailyBalances, EnhancedDailyBalance::balance))
                .averagePriceForTOz(averagePrice)
                .interestRate(interestRate)
                .numOfDays(numOfDays)
                .daysOfYear(region == Region.CN ? 360 : 365)
                .owner(owner)
                .holder(holder)
                .build();
        logger.info("daily balances used for calculation: {}", dailyBalances);
        logger.info("interest generated: final amount -> {}; details -> {}", interest.value(), interest);
        return interest;
    }

    private Double average(List<EnhancedDailyBalance> dailyBalances, ToDoubleFunction<EnhancedDailyBalance> mapper) {
        var summedByDay = dailyBalances.stream().collect(groupingBy(EnhancedDailyBalance::date, summingDouble(mapper)));
        return summedByDay.values().stream().mapToDouble(Double::doubleValue).average().orElse(0d);
    }

    private static class GroupingKeys {
        final String group;
        final String interestKey;
        final String metal;

        private GroupingKeys(String group, String interestKey, String metal) {
            this.group = group;
            this.interestKey = interestKey;
            this.metal = metal;
        }
    }
}

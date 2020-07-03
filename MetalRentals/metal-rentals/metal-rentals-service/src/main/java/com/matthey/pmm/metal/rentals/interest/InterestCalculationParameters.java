package com.matthey.pmm.metal.rentals.interest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;
import com.matthey.pmm.metal.rentals.Account;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Immutable
@JsonDeserialize(as = ImmutableInterestCalculationParameters.class)
@JsonSerialize(as = ImmutableInterestCalculationParameters.class)
public interface InterestCalculationParameters {

    Map<String, Set<Account>> accounts();

    Map<String, Double> interestRates();

    Map<String, Map<String, Double>> averagePrices();

    @Derived
    default Map<String, Account> accountMap() {
        return Maps.uniqueIndex(accounts().values().stream().flatMap(Set::stream).collect(toList()), Account::name);
    }
}

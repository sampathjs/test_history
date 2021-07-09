package com.matthey.pmm.metal.transfers.interest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Style;

import static org.immutables.value.Value.Immutable;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableInterest.class)
@JsonSerialize(as = ImmutableInterest.class)
public interface Interest {

    String group();

    String account();

    String metal();

    String unit();

    String currency();

    Double averageBalanceInTOz();

    Double averageBalance();

    Double averagePriceForTOz();

    Double interestRate();

    Integer numOfDays();

    Integer daysOfYear();

    String owner();

    String holder();

    @Derived
    default Double averagePrice() {
        return averageBalanceInTOz() * averagePriceForTOz() / averageBalance();
    }

    @Derived
    default Double value() {
        return -1 * averageBalanceInTOz() * averagePriceForTOz() * interestRate() * numOfDays() / daysOfYear();
    }
}

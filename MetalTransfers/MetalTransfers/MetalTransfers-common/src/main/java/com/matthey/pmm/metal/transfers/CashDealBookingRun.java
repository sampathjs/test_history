package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableCashDealBookingRun.class)
@JsonSerialize(as = ImmutableCashDealBookingRun.class)
public abstract class CashDealBookingRun extends Run {

    public abstract CashDeal deal();
}

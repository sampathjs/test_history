package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableCashDealKey.class)
@JsonSerialize(as = ImmutableCashDealKey.class)
public interface CashDealKey {

    String cashflowType();

    String internalBU();

    String externalBU();

    String settleDate();

    String position();
}

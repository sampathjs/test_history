package com.matthey.pmm.metal.rentals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Style;
import org.jetbrains.annotations.Nullable;

import static org.immutables.value.Value.Immutable;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableCashDeal.class)
@JsonSerialize(as = ImmutableCashDeal.class)
public interface CashDeal {

    String currency();

    String cashflowType();

    String internalBU();

    String internalPortfolio();

    String externalBU();

    @Nullable
    String externalPortfolio();

    String settleDate();

    String statementDate();

    Double position();

    @Nullable
    Double fxRate();

    @Nullable
    @Value.Auxiliary
    Boolean isCnAndHasVat();

    @Nullable
    @Value.Auxiliary
    String tranNum();
}

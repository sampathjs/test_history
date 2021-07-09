package com.matthey.pmm.metal.transfers.mock;

import static org.immutables.value.Value.Immutable;

@Immutable
public interface DailyBalanceSelector {

    String account();

    String accountType();

    String date();
}

package com.matthey.pmm.mock;

import static org.immutables.value.Value.Immutable;

@Immutable
public interface DailyBalanceSelector {

    String account();

    String accountType();

    String date();
}

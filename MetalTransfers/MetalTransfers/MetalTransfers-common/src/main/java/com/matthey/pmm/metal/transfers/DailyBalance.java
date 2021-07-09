package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Style;

import static org.immutables.value.Value.Immutable;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableDailyBalance.class)
@JsonSerialize(as = ImmutableDailyBalance.class)
public interface DailyBalance {

    String account();

    String date();

    String metal();

    Double balance();

    Double balanceInTOz();
}

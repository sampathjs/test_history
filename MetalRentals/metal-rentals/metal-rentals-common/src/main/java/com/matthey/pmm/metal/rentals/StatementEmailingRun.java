package com.matthey.pmm.metal.rentals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableStatementEmailingRun.class)
@JsonSerialize(as = ImmutableStatementEmailingRun.class)
public abstract class StatementEmailingRun extends Run {

    public abstract PartyContact partyContact();

    public abstract String statementPath();
}

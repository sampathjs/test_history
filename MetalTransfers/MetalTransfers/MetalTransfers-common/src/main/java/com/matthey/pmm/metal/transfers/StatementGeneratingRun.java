package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableStatementGeneratingRun.class)
@JsonSerialize(as = ImmutableStatementGeneratingRun.class)
public abstract class StatementGeneratingRun extends Run {

    public abstract String party();

    public abstract String accountGroup();

    public abstract String statementPath();
}

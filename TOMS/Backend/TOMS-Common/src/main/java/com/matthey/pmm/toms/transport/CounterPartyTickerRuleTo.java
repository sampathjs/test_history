package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Contains a row in the Counter Party <-> TOMS Product <-> Loco <-> Form <-> Account Name
 * mapping
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableCounterPartyTickerRuleTo.class)
@JsonDeserialize(as = ImmutableCounterPartyTickerRuleTo.class)
@JsonRootName (value = "counterparty_ticker_rule")
public abstract class CounterPartyTickerRuleTo {  
    public abstract long idCounterParty();

    @Auxiliary
    @Nullable
    public abstract String counterPartyDisplayName();
    
    public abstract long idTicker();

    @Auxiliary
    @Nullable
    public abstract String tickerDisplayName();
    
    public abstract long idMetalLocation();

    @Auxiliary
    @Nullable
    public abstract String metalLocationDisplayString();

    public abstract long idMetalForm();

    @Auxiliary
    @Nullable
    public abstract String metalFormDisplayString();

    public abstract String accountName();
}

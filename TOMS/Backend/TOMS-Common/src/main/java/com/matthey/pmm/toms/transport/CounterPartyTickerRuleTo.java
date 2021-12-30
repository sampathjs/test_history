package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
    public abstract String counterPartyDisplayString();
    
    public abstract long idTicker();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
    public abstract String tickerDisplayName();
    
    public abstract long idMetalLocation();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
    public abstract String metalLocationDisplayString();

    public abstract long idMetalForm();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
    public abstract String metalFormDisplayString();

    public abstract String accountName();
}

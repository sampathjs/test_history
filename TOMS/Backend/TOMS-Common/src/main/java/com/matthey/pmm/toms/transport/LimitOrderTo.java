package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A Limit Order for TOMS (Transfer Object)
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableLimitOrderTo.class)
@JsonDeserialize(as = ImmutableLimitOrderTo.class)
@JsonRootName (value = "limitOrder")
public abstract class LimitOrderTo extends OrderTo {  
    @Auxiliary
    @Nullable
    public abstract String settleDate();
    
    @Auxiliary
    public abstract int idExpirationStatus();
    
    @Auxiliary
    public abstract double price();

//    @Auxiliary
//    public abstract int idCurrency();

    @Auxiliary
    public abstract int idYesNoPartFillable();

    @Auxiliary
    public abstract double spotPrice();

    @Auxiliary
    public abstract int idStopTriggerType();

    @Auxiliary
    public abstract int idCurrencyCrossMetal();

    @Auxiliary
    @Nullable
    public abstract Double executionLikelihood();
    
    @Auxiliary
    @Nullable
    public abstract List<Integer> fillIds();
}

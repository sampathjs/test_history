package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableIndex;

/**
 * Index setup for two currencies (either metal or normal currency).
 * The index consists of a reference to the index name and
 * references to the two currencies the index is being defined for. 
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableLimitOrder.class)
@JsonDeserialize(as = ImmutableLimitOrder.class)
@JacksonXmlRootElement(localName = "LimitOrder")
public abstract class LimitOrder extends Order {  
    @Auxiliary
    public abstract int idPriceType();

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
}

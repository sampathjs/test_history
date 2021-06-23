package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Party entity, in context of TOMS usually a counterparty having an ID, a name and a type.
 * Maintained by Endur
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderCreditCheckTo.class)
@JsonDeserialize(as = ImmutableOrderCreditCheckTo.class)
@JsonRootName(value = "orderCreditCheck")
public abstract class OrderCreditCheckTo {
	/**
	 * Endur side ID
	 * @return
	 */
    public abstract int id(); 
   
    @Auxiliary
    public abstract int idParty();
    
    @Auxiliary
    @Nullable
    public abstract Double creditLimit();

    @Auxiliary
    @Nullable
    public abstract Double currentUtilization();

    @Auxiliary
    @Nullable
    public abstract String runDateTime();

    @Auxiliary
    public abstract int idCreditCheckRunStatus();

    @Auxiliary
    @Nullable
    public abstract Integer idCreditCheckOutcome();
}

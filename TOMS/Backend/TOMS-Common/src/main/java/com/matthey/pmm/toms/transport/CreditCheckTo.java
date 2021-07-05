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
@JsonSerialize(as = ImmutableCreditCheckTo.class)
@JsonDeserialize(as = ImmutableCreditCheckTo.class)
@JsonRootName(value = "orderCreditCheck")
public abstract class CreditCheckTo {
	/**
	 * Endur side ID
	 * @return
	 */
    public abstract long id(); 
   
    @Auxiliary
    public abstract long idParty();
    
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
    public abstract long idCreditCheckRunStatus();

    @Auxiliary
    @Nullable
    public abstract Long idCreditCheckOutcome();
}

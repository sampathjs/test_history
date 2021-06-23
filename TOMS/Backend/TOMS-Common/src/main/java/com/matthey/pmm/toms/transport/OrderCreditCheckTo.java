package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;


/**
 * Party entity, in context of TOMS usually a counterparty having an ID, a name and a type.
 * Maintained by Endur
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderCreditCheckTo.class)
@JsonDeserialize(as = ImmutableOrderCreditCheckTo.class)
@JacksonXmlRootElement(localName = "OrderCreditCheck")
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

package com.matthey.pmm.toms.transport;

import java.util.Arrays;
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
	/*
	 * The following lists contain the attributes that are not allowed to get changed for certain status transitions.
	 * Remember to update the lists if the attribute names are refactored, attributes are getting added or removed.
	 */
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_CONFIRMED = Arrays.asList(
			  "settleDate", "idExpirationStatus", "price", "idYesNoPartFillable", "spotPrice",
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);
	
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

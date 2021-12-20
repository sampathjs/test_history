package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A Reference Order for TOMS.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceOrderLegTo.class)
@JsonDeserialize(as = ImmutableReferenceOrderLegTo.class)
@JsonRootName (value = "referenceOrderLeg")
public abstract class ReferenceOrderLegTo {
	/**
	 * TOMS maintained ID 
	 */	
	public abstract long id();
	
	@Auxiliary
	@Nullable
	public abstract Double notional();
	
    @Auxiliary
    @Nullable
    public abstract String fixingStartDate();
	
    @Auxiliary
    @Nullable
    public abstract String fixingEndDate();
    
    @Nullable
    @Auxiliary
	public abstract Long idPaymentOffset();
    
	@Auxiliary
    @Nullable
	public abstract String displayStringPaymentOffset();
	
    @Nullable
    @Auxiliary
	public abstract Long idSettleCurrency();
    
	@Auxiliary
    @Nullable
	public abstract String displayStringSettleCurrency();
    
    @Auxiliary
	public abstract long idRefSource();

    @Nullable
	@Auxiliary
	public abstract String displayStringRefSource();
    
    @Auxiliary
	public abstract long idFxIndexRefSource();
    
    @Nullable
	@Auxiliary
	public abstract String displayStringFxIndexRefSource();    
}

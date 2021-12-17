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
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_FILLED = Arrays.asList(
			  "settleDate", "limitPrice", 
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED = Arrays.asList(
			  "settleDate", "limitPrice", 
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED = Arrays.asList(
			  "settleDate", "limitPrice", 
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_EXPIRED = Arrays.asList(
			  "settleDate", "limitPrice",
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);	
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_CANCELLED_EXPIRED = Arrays.asList(
			  "settleDate", "limitPrice",
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PULLED = Arrays.asList(
			  "settleDate", "limitPrice",
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_REJECTED = Arrays.asList(
			  "settleDate", "limitPrice",
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_MATURED = Arrays.asList(
			  "settleDate", "limitPrice",
			  "idStopTriggerType", "idCurrencyCrossMetal", "executionLikelihood"
			);

	
	@Auxiliary
    @Nullable
    public abstract String settleDate();

	@Auxiliary
    @Nullable
	public abstract String startDateConcrete();

	@Auxiliary
    @Nullable
	public abstract Long idStartDateSymbolic();
	
	@Auxiliary
    @Nullable
	public abstract String displayStringStartDateSymbolic();	
	
    @Auxiliary
    public abstract double limitPrice();

    @Auxiliary
    @Nullable
    public abstract Long idPriceType();

	@Auxiliary
    @Nullable
	public abstract String displayStringPriceType();
    
    @Auxiliary
    @Nullable
    public abstract Long idYesNoPartFillable();
    
	@Auxiliary
    @Nullable
	public abstract String displayStringPartFillable();	

    @Auxiliary
    @Nullable
    public abstract Long idStopTriggerType();
    
	@Auxiliary
    @Nullable
	public abstract String displayStringStopTriggerType();

    @Auxiliary
    @Nullable
    public abstract Long idCurrencyCrossMetal();
    
	@Auxiliary
    @Nullable
	public abstract String displayStringCurrencyCrossMetal();

    @Auxiliary
    @Nullable
    public abstract Long idValidationType();
    
	@Auxiliary
    @Nullable
	public abstract String displayStringValidationType();	


    @Auxiliary
    @Nullable
    public abstract String expiryDate();
    
    @Auxiliary
    @Nullable
    public abstract Double executionLikelihood();
}

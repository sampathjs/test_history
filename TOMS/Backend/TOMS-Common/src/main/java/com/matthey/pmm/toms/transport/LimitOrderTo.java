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
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_IN_MEMORY_TO_PENDING = Arrays.asList(
			 );
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PENDING = Arrays.asList(
			"idOrderType", "id", "createdAt", "idOrderStatus", "idCreatedByUser"
			 );

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED = Arrays.asList(
			"idBuySell", "idOrderType", "reference", "idInternalBu", "idExternalBu", "idTicker", "idBaseQuantityUnit", 
			"baseQuantity", "idPriceType", "idContractType", "limitPrice", "startDateConcrete", "idStartDateSymbolic", "settleDate", 
			"idValidationType", "expiryDate", "idYesNoPartFillable", "idMetalLocation", "idMetalForm", "orderCommentIds", 
			"id", "createdAt", "idCreatedByUser"
			 );

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_REJECTED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_CONFIRMED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_PENDING = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PENDING;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_EXPIRED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_CANCELLED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_PART_FILLED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_FILLED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PART_FILLED_TO_EXPIRED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PART_FILLED_TO_PART_CANCELLED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PART_FILLED_TO_FILLED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_FILLED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PULLED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_REJECTED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_EXPIRED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PART_EXPIRED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PART_CANCELLED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

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

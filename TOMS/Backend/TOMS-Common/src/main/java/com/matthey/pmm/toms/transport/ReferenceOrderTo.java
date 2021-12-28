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
 * A Reference Order for TOMS.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceOrderTo.class)
@JsonDeserialize(as = ImmutableReferenceOrderTo.class)
@JsonRootName (value = "referenceOrder")
public abstract class ReferenceOrderTo extends OrderTo {  
	/*
	 * The following lists contain the attributes that are not allowed to get changed for certain status transitions.
	 * Remember to update the lists if the attribute names are refactored, attributes are getting added or removed.
	 */
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_IN_MEMORY_TO_PENDING = Arrays.asList(
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PENDING = Arrays.asList(
			"id", "createdAt", "idCreatedByUser", "idOrderStatus", "idOrderType");

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED = Arrays.asList(
			"idBuySell", "idOrderType", "reference", "idInternalBu", "idExternalBu", "idTicker", "idBaseQuantityUnit", "baseQuantity", "idContractType", 
			"idMetalLocation", "idMetalForm", "metalPriceSpread", "fxRateSpread", "contangoBackwardation", "orderCommentIds", 
			"id", "createdAt", "idCreatedByUser"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_REJECTED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PENDING_TO_CONFIRMED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_FILLED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_FILLED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PULLED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_REJECTED_TO_MATURED = UNCHANGEABLE_ATTRIBUTES_PENDING_TO_PULLED;
	
	@Auxiliary
	@Nullable
	public abstract Double metalPriceSpread();
	
	@Auxiliary
	@Nullable
	public abstract Double fxRateSpread();

	@Auxiliary
	@Nullable
	public abstract Double contangoBackwardation();
		
    @Auxiliary
    @Nullable
    public abstract List<Long> legIds();
}

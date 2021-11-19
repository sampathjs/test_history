package com.matthey.pmm.toms.transport;

import java.util.Arrays;
import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName(value = "referenceOrder")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public abstract class ReferenceOrderTo extends OrderTo {  
	/*
	 * The following lists contain the attributes that are not allowed to get changed for certain status transitions.
	 * Remember to update the lists if the attribute names are refactored, attributes are getting added or removed.
	 */
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED = Arrays.asList(
			"metalPriceSpread", "fxRateSpread", "contangoBackwardation", "idContractType", 
			"legIds"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED = Arrays.asList(
			"metalPriceSpread", "fxRateSpread", "contangoBackwardation", "idContractType", 
			"legIds"
			);

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_FILLED = Arrays.asList(
			"metalPriceSpread", "fxRateSpread", "contangoBackwardation", "idContractType", 
			"legIds"
			);
	
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
	public abstract Long idContractType();
	
	@Auxiliary
	@Nullable
	public abstract String displayStringContractType();	
	
    @Auxiliary
    @Nullable
    public abstract List<Long> legIds();
}

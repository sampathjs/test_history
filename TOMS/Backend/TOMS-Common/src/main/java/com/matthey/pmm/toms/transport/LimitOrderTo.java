package com.matthey.pmm.toms.transport;

import java.util.Arrays;
import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A Limit Order for TOMS (Transfer Object)
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableLimitOrderTo.class)
@JsonDeserialize(as = ImmutableLimitOrderTo.class)
@JsonRootName (value = "limitOrder")
@ApiModel(value = "LimitOrder", description = "The TO representation of a LimitOrder.",
	parent = OrderTo.class)
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
	@ApiModelProperty(value = "The settlement date of the LimitOrder in yyyy-MM-dd format",
		allowEmptyValue = true,
		required = false)
    public abstract String settleDate();

	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "A concrete start date of the LimitOrder in yyyy-MM-dd format. Either this value or idStartDateSymbolic has to be provided.",
		allowEmptyValue = true,
		required = false)
	public abstract String startDateConcrete();

	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "A symbolic start date of the LimitOrder. Either this value or startDateConcrete has to be provided. The IDs capture instances of Reference having ReferenceType #25 (Symbolic Date)",
		allowEmptyValue = true,
		required = false)
	public abstract Long idStartDateSymbolic();
	
	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the symbolic date having ID idStartDateSymbolic. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)    
	public abstract String displayStringStartDateSymbolic();	
	
    @Auxiliary
	@ApiModelProperty(value = "The price limit (Min / Max) depending on order direction (Buy / Sell)",
		allowEmptyValue = false,
		required = true)
    public abstract double limitPrice();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The price type of the LimitOrder. The IDs capture instances of Reference having ReferenceType #15 (Price Type). Available values: #105(Spot Price), #106 (Forward Price), #159 (Spot+ Market Swap Rate)",
		allowEmptyValue = true,
		required = false,
		allowableValues = "105, 106, 159")
    public abstract Long idPriceType();

	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the Price Type having ID idPriceType. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)    
	public abstract String displayStringPriceType();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Whether the LimitOrder can be filled partially or not. The IDs capture instances of Reference having ReferenceType #12 (Yes/No). Available values: #97(Yes), #98 (No)",
		allowEmptyValue = true,
		required = false,
		allowableValues = "97, 98")
    public abstract Long idYesNoPartFillable();
    
	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The value of idYesNoPartFillable as a name. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)        
	public abstract String displayStringPartFillable();	

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The IDs capture instances of Reference having ReferenceType #17 (Stop Trigger Type).",
		allowEmptyValue = true,
		required = false)
    public abstract Long idStopTriggerType();
    
	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The value of idStopTriggerType as a name. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)        
	public abstract String displayStringStopTriggerType();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the cross currency field. The ID is instance of a Reference having ReferenceType #9 (Metal) or ReferenceRype #10 (Currency)",
		allowEmptyValue = false,
		required = true)     
    public abstract Long idCurrencyCrossMetal();
    
	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The value of idCurrencyCrossMetal as a name. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)         
	public abstract String displayStringCurrencyCrossMetal();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The IDs capture instances of Reference having ReferenceType #24 (Validation Type). Allowed Values: #184 (Good Til Cancelled), #185 (Expiry Date)",
		allowEmptyValue = true,
		required = false,
		allowableValues = "184, 185")
    public abstract Long idValidationType();

	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The value of idValidationType as a name. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringValidationType();	


    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The expiration date of the LimitOrder in yyyy-MM-dd format",
		allowEmptyValue = true,
		required = false)
    public abstract String expiryDate();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The execution likelihood as real number from including 0 to 1",
		allowEmptyValue = true,
		required = false,
		allowableValues = "range[0,1]")
    public abstract Double executionLikelihood();
}

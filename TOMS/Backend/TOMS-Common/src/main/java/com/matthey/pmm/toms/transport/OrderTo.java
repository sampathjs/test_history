package com.matthey.pmm.toms.transport;

import java.util.Arrays;
import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract base class for Limit Orders and Reference Orders
 * @author jwaechter
 * @version 1.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value=LimitOrderTo.class, name = "LimitOrder"),
  @JsonSubTypes.Type(value=ReferenceOrderTo.class, name = "ReferenceOrder")
})
public abstract class OrderTo {
	/*
	 * The following lists contain the attributes that are not allowed to get changed for certain status transitions.
	 * Remember to update the lists if the attribute names are refactored, attributes are getting added or removed.
	 */
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");
	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_FILLED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");

	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"idYesNoPhysicalDeliveryRequired", "createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");

	
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CONFIRMED_TO_CANCELLED_EXPIRED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_PULLED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_REJECTED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_MATURED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");

	public static final List<String> UNCHANGEABLE_ATTRIBUTES_EXPIRED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", 
					"createdAt", "idCreatedByUser", "idPriceType",
					"reference", "idMetalForm", "idMetalLocation");
	
	/**
	 * TOMS maintained ID 
	 */	
	public abstract long id();

	public abstract int version();
	
    @Auxiliary
    public abstract long idInternalBu();

	@Auxiliary
	@Nullable
	public abstract String displayStringInternalBu(); 
    
    @Auxiliary
	@Nullable
    public abstract Long idExternalBu();

	@Auxiliary
	@Nullable
	public abstract String displayStringExternalBu(); 

    @Auxiliary
	@Nullable
    public abstract Long idInternalLe();
    
	@Auxiliary
	@Nullable
	public abstract String displayStringInternalLe(); 

	@Auxiliary
	@Nullable
    public abstract Long idExternalLe();

	@Auxiliary
	@Nullable
	public abstract String displayStringExternalLe();
	
	@Auxiliary
	@Nullable
    public abstract Long idIntPortfolio();
	
	@Auxiliary
	@Nullable
	public abstract String displayStringIntPortfolio();

	@Auxiliary
	@Nullable
    public abstract Long idExtPortfolio();

	@Auxiliary
	@Nullable
	public abstract String displayStringExtPortfolio();

	@Auxiliary
    public abstract long idBuySell();
	
	@Auxiliary
	@Nullable
	public abstract String displayStringBuySell();
    
    @Auxiliary
    public abstract long idBaseCurrency();
    
	@Auxiliary
	@Nullable
	public abstract String displayStringBaseCurrency();
    
    @Nullable
    @Auxiliary
    public abstract Double baseQuantity();

    @Auxiliary
    @Nullable
    public abstract Long idBaseQuantityUnit();

	@Auxiliary
	@Nullable
	public abstract String displayStringBaseQuantityUnit();
    
    @Auxiliary
    public abstract Long idTermCurrency();
    
	@Auxiliary
	@Nullable
	public abstract String displayStringTermCurrency();


    @Auxiliary
    @Nullable
    public abstract String reference();    
    
    @Auxiliary
    @Nullable
    public abstract Long idMetalForm();

	@Auxiliary
	@Nullable
	public abstract String displayStringMetalForm();
    
    @Auxiliary
    @Nullable
    public abstract Long idMetalLocation();

	@Auxiliary
	@Nullable
	public abstract String displayStringMetalLocation();

    
    @Auxiliary
    public abstract Long idOrderStatus();

	@Auxiliary
	@Nullable
	public abstract String displayStringOrderStatus();
    
    @Auxiliary
    @Nullable
    public abstract List<Long> creditChecksIds();
    
    @Auxiliary
    public abstract String createdAt();
    
    @Auxiliary
    public abstract long idCreatedByUser();
    
	@Auxiliary
	@Nullable
	public abstract String displayStringCreatedByUser();
    
    @Auxiliary
    public abstract String lastUpdate();

    @Auxiliary
    public abstract long idUpdatedByUser();
    
	@Auxiliary
	@Nullable
	public abstract String displayStringUpdatedByUser();    

    @Auxiliary
    public abstract double fillPercentage();
    
	@Auxiliary
	@Nullable
	public abstract String displayStringTicker();     

	@Auxiliary
	@Nullable
	public abstract Long idTicker(); 

	@Auxiliary
	public abstract Long idContractType();

	@Auxiliary
	@Nullable
	public abstract String displayStringContractType();
	
    @Auxiliary
    @Nullable
    public abstract List<Long> orderCommentIds();
    
    @Auxiliary
    @Nullable
    public abstract List<Long> fillIds();
}

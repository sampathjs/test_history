package com.matthey.pmm.toms.transport;

import java.util.Arrays;
import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for Limit Orders and Reference Orders
 * @author jwaechter
 * @version 1.0
 */
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
    public abstract Long idExternalBu();

    @Auxiliary
	@Nullable
    public abstract Long idInternalLe();

	@Auxiliary
	@Nullable
    public abstract Long idExternalLe();
	
	@Auxiliary
	@Nullable
    public abstract Long idIntPortfolio();

	@Auxiliary
	@Nullable
    public abstract Long idExtPortfolio();

    @Auxiliary
    public abstract long idBuySell();
    
    @Auxiliary
    public abstract long idBaseCurrency();
    
    @Nullable
    @Auxiliary
    public abstract Double baseQuantity();

    @Auxiliary
    @Nullable
    public abstract Long idBaseQuantityUnit();

    @Auxiliary
    public abstract Long idTermCurrency();

    @Auxiliary
    @Nullable
    public abstract String reference();    
    
    @Auxiliary
    @Nullable
    public abstract Long idMetalForm();
    
    @Auxiliary
    @Nullable
    public abstract Long idMetalLocation();
   
    @Auxiliary
    public abstract Long idOrderStatus();
        
    @Auxiliary
    @Nullable
    public abstract List<Long> creditChecksIds();

    @Auxiliary
    public abstract String createdAt();
    
    @Auxiliary
    public abstract long idCreatedByUser();
    
    @Auxiliary
    public abstract String lastUpdate();

    @Auxiliary
    public abstract long idUpdatedByUser();
    
    @Auxiliary
    @Nullable
    public abstract List<Long> orderCommentIds();
    
    @Auxiliary
    @Nullable
    public abstract List<Long> fillIds();
}

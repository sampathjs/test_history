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
	public static final List<String> UNCHANGEABLE_ATTRIBUTES_CANCELLED_FILLED_APPROVED = 
			Arrays.asList("idInternalBu", "idExternalBu","idInternalLe", "idExternalLe",
					"idIntPortfolio", "idExtPortfolio", "idBuySell", "idBaseCurrency",
					"baseQuantity", "idBaseQuantityUnit", "idTermCurrency", "idPaymentPeriod",
					"idYesNoPhysicalDeliveryRequired", "createdAt", "idCreatedByUser", "idPriceType");
	
	/**
	 * TOMS maintained ID 
	 */	
	public abstract int id();

    @Auxiliary
	public abstract int version();
	
    @Auxiliary
    public abstract int idInternalBu();

	@Auxiliary
    public abstract int idExternalBu();

    @Auxiliary
    public abstract int idInternalLe();

	@Auxiliary
    public abstract int idExternalLe();
	
	@Auxiliary
	@Nullable
    public abstract Integer idIntPortfolio();

	@Auxiliary
	@Nullable
    public abstract Integer idExtPortfolio();

    @Auxiliary
    public abstract int idBuySell();
    
    @Auxiliary
    public abstract int idBaseCurrency();
    
    @Nullable
    @Auxiliary
    public abstract Double baseQuantity();

    @Auxiliary
    @Nullable
    public abstract Integer idBaseQuantityUnit();

    @Auxiliary
    public abstract int idTermCurrency();
   
    @Auxiliary
    public abstract int idPaymentPeriod();

    @Auxiliary
    public abstract int idYesNoPhysicalDeliveryRequired();

    @Auxiliary
    public abstract int idOrderStatus();
    
    @Auxiliary
    public abstract int idPriceType();
    
    @Auxiliary
    @Nullable
    public abstract List<Integer> creditChecksIds();

    @Auxiliary
    public abstract String createdAt();
    
    @Auxiliary
    public abstract int idCreatedByUser();
    
    @Auxiliary
    public abstract String lastUpdate();

    @Auxiliary
    public abstract int idUpdatedByUser();
    
    @Auxiliary
    @Nullable
    public abstract List<Integer> orderCommentIds();
}

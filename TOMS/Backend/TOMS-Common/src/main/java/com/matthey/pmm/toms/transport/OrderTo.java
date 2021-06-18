package com.matthey.pmm.toms.transport;

import java.util.Date;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Abstract base class for Limit Orders and Reference Orders
 * @author jwaechter
 * @version 1.0
 */
public abstract class OrderTo {
	/**
	 * Endur ID
	 * @return
	 */
	public abstract int id();
   

    @Auxiliary
    public abstract int idInternalParty();

	@Auxiliary
    public abstract int idExternalParty();

    @Auxiliary
    public abstract int idBuySell();
    
    @Auxiliary
    public abstract int idMetalCurrency();
    
    @Auxiliary
    public abstract double quantity();

    @Auxiliary
    public abstract int idQuantityUnit();

    @Auxiliary
    public abstract int idCurrency();

    @Auxiliary
    public abstract int idOrderType();

    @Auxiliary
    public abstract int idPaymentPeriod();

    @Auxiliary
    public abstract int idYesNoPhysicalDeliveryRequired();

    @Auxiliary
    public abstract int idOrderStatus();

    @Auxiliary
    public abstract String createdAt();
    
    @Auxiliary
    public abstract int idCreatedByUser();
    
    @Auxiliary
    public abstract String lastUpdate();

    @Auxiliary
    public abstract int idUpdatedByUser();
    
    @Auxiliary
    public abstract int idPriceType();
}

package com.matthey.pmm.toms.model;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.matthey.pmm.toms.model.ImmutableBuySell;

/**
 * Order Status , pair of order status name and order type. 
 * TOMS maintained.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableBuySell.class)
@JsonDeserialize(as = ImmutableBuySell.class)
@JacksonXmlRootElement(localName = "BuySell")
public abstract class BuySell {
	/**
	 * Endur side ID.
	 * @return
	 */
	public abstract int id();
   
    @Auxiliary
    public abstract int idBuySellReference();
}

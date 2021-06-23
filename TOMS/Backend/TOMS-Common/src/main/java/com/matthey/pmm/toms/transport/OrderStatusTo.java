package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Order Status , pair of order status name and order type. 
 * TOMS maintained.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderStatusTo.class)
@JsonDeserialize(as = ImmutableOrderStatusTo.class)
@JacksonXmlRootElement(localName = "OrderStatus")
public abstract class OrderStatusTo {
    public abstract int id();
   
    @Auxiliary
    public abstract int idOrderStatusName();

    @Auxiliary
    public abstract int idOrderTypeName();
}

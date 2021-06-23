package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Order Status , pair of order status name and order type. 
 * TOMS maintained.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderStatusTo.class)
@JsonDeserialize(as = ImmutableOrderStatusTo.class)
@JsonRootName (value = "orderStatus")
public abstract class OrderStatusTo {
    public abstract int id();
   
    @Auxiliary
    public abstract int idOrderStatusName();

    @Auxiliary
    public abstract int idOrderTypeName();
}

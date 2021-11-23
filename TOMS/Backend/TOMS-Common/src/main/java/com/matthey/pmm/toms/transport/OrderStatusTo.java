package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

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
    public abstract long id();
   
    @Auxiliary
    public abstract long idOrderStatusName();

    @Auxiliary
    public abstract long idOrderTypeName();

    @Auxiliary
    public abstract long idOrderTypeCategory();
    
    @Auxiliary
    @Nullable
    public abstract Long sortColumn();
}

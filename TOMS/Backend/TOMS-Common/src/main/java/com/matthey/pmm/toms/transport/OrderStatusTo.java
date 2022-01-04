package com.matthey.pmm.toms.transport;

import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
@ApiModel(value = "OrderStatus", description = "The TO representation of an Order Status")
@Value.Style (jdkOnly = true)
public abstract class OrderStatusTo {
	@ApiModelProperty(value = "The order management system internal unique ID for the order status",
			allowEmptyValue = false,
			required = true)		
    public abstract long id();
   
    @Auxiliary
	@ApiModelProperty(value = "The ID of the order status name. The IDs are Reference IDs of ReferenceType #4 (Order Status):"
			+  " 7(Pending), 9(Filled), 10(Cancelled), 11(Waiting Approval), 12 (Confirmed), 113 (Part Cancelled), 153 (Rejected)"
			+  " 154 (Part Filled), 155 (Part Expired), 156 (Expired), 157 (Matured), 163 (Pulled), 377 (In Memory)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "7, 9, 10, 11, 12, 113, 153, 154, 155, 156, 157, 163, 377")  
    public abstract long idOrderStatusName();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the order type. The IDs are Reference IDs of ReferenceType #2 (Order Type):"
			+  " 13(Limit Order), 14(Reference Order), ",
			allowEmptyValue = false,
			required = true,
			allowableValues = "13, 14")   
    public abstract long idOrderTypeName();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the order type. The IDs are Reference IDs of ReferenceType #29 (Order Type Category):"
			+  " 229(Open), 230(Closed), ",
			allowEmptyValue = false,
			required = true,
			allowableValues = "13, 14")   
    public abstract long idOrderTypeCategory();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional custom sort information for the frontend to allow the display order of elements being controlled by the backend",
		allowEmptyValue = true,
		required = false)	    
    public abstract Long sortColumn();
}

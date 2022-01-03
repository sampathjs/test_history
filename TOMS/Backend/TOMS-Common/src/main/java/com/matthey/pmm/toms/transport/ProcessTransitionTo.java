package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Immutable
@JsonSerialize(as = ImmutableProcessTransitionTo.class)
@JsonDeserialize(as = ImmutableProcessTransitionTo.class)
@JsonRootName (value = "processTransition")
@ApiModel(value = "ProcessTransition", description = "The TO representation an abstract transition between two status. As in terms of graphs it can be considered an edge.")
public abstract class ProcessTransitionTo {
	@ApiModelProperty(value = "The order management system internal unique ID for the process transition",
			allowEmptyValue = false,
			required = true)		
    public abstract long id();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the category of the status transition. The IDs are Reference IDs of ReferenceType #18 (Process Transition Type):"
			+  " 111(Transition for Limit Orders), 112(Transition for Reference Orders), 117 (Transition for Credit Check Run Status),"
			+  " 289 (Transition for Fill Status), 378 (Email Status Transition)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "111, 112, 117, 289, 378")  
    public abstract long referenceCategoryId();

    @Auxiliary
	@ApiModelProperty(value = "The ID designating the starting status (node) of the process transition. The referenced entity is depending of the "
		+ "referenceCategoryId of the transition, it can be for example the ID of an OrderStatus or just a Reference ID" ,
		allowEmptyValue = false,
		required = true)
    public abstract long fromStatusId();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID designating the target status (node) of the process transition. The referenced entity is depending of the "
			+ "referenceCategoryId of the transition, it can be for example the ID of an OrderStatus or just a Reference ID" ,
			allowEmptyValue = false,
			required = true)
    public abstract long toStatusId();
    
    /**
     * Attributes of the class the process transition is setup for that are not allowed to change. 
     * @return
     */
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The list of attributes of the underlying TO that are NOT allowed to be changed when doing the process transition."
		+ " The provided values are depending on the referenceCategoryId. For example, for referenceCategoryId=111 the value 'createdAt' "
		+ " indicates the createdAt field of the LimitOrder is not allowed to change.",
		allowEmptyValue = true,
		required = false)    
    public abstract List<String> unchangeableAttributes();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional custom sort information for the frontend to allow the display order of elements being controlled by the backend",
		allowEmptyValue = true,
		required = false)	    
    public abstract Long sortColumn();
}

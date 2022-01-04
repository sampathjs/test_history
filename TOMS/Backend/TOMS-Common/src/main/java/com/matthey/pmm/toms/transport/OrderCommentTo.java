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
 * Class containing a comment for an order.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableOrderCommentTo.class)
@JsonDeserialize(as = ImmutableOrderCommentTo.class)
@JsonRootName(value = "orderComment")
@ApiModel(value = "OrderComment", description = "The TO representation of a comment for both Limit and Reference Orders.")
@Value.Style (jdkOnly = true)
public abstract class OrderCommentTo {	
	/**
	 * TOMS maintained ID 
	 */	
	@ApiModelProperty(value = "The order management system internal unique ID for this comment",
			allowEmptyValue = false,
			required = true)	
	public abstract long id();

    @Auxiliary
	@ApiModelProperty(value = "The text of the order comment. Current maximal supported length is 1000 characters",
		allowEmptyValue = false,
		required = true)	    
	public abstract String commentText();
	
    @Auxiliary
	@ApiModelProperty(value = "The timestamp of the creation date. The value is set by the backend when submitting the order in pending state, but has to be provided by the frontend as well.",
		allowEmptyValue = false,
		required = true)    
    public abstract String createdAt();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has created the order. Assigned by the backend, but required to be provided by the fronted as well.",
		allowEmptyValue = false,
		required = true)    
    public abstract long idCreatedByUser();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the user who has created the order. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
    public abstract String displayStringCreatedByUser();
    
    @Auxiliary
	@ApiModelProperty(value = "The timestamp of the last time the order was updated. The value is set by the backend when submitting the order, but has to be provided by the frontend as well.",
		allowEmptyValue = false,
		required = true)
    public abstract String lastUpdate();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has updated the order last time. Assigned by the backend, but required to be provided by the fronted as well.",
		allowEmptyValue = false,
		required = true)
    public abstract long idUpdatedByUser();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the user who has updated the order last time. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
    public abstract String displayStringUpdatedByUser();
    
    @Auxiliary
	@ApiModelProperty(value = "The status of the comment (deleted, or valid). Allowed values are of reference type #35 (Lifecycle Status): 290(Authorisation Pending), 291(Authorised and Active), 292 (Authorised and Inactive), 293(Deleted)",
		allowEmptyValue = false,
		required = true,
	allowableValues = "290, 291, 292, 293")
    public abstract long idLifeCycle();
}

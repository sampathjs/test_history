package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
 * Contains a single pair of ID / name for constants used throughout the system. 
 * A single referenced can be interpreted as a single value in  a list of sibling values 
 * sharing the same type that together form a pick list.
 * Maintained by TOMS. Contains optional mappings to Endur IDs. Endur IDs not being used have value -1.
 * @author jwaechter
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceTo.class)
@JsonDeserialize(as = ImmutableReferenceTo.class)
@JsonRootName (value = "reference")
@ApiModel(value = "Reference", description = "The TO representation of a Reference")
public abstract class ReferenceTo {
	@ApiModelProperty(value = "The order management system internal unique ID for the Reference",
			allowEmptyValue = false,
			required = true)
	public abstract long id();
	
	@ApiModelProperty(value = "The ID of the type of the reference. The IDs link to ReferenceType.",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[1,35]")		
	public abstract long idType();
	
    @Auxiliary
	@ApiModelProperty(value = "The name of the reference.",
		allowEmptyValue = false,
		required = true)
	public abstract String name();

    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "An optional name used for display to the user in case it is necessary to use a string different to name in the frontend",
		allowEmptyValue = true,
		required = false)
	public abstract String displayName();

    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "An optional endur ID in case the reference is maintained by Endur",
		allowEmptyValue = true,
		required = false)
    public abstract Long endurId();
    
    @Auxiliary
	@ApiModelProperty(value = "The status of the reference (deleted, or valid). Allowed values are of reference type #35 (Lifecycle Status): 290(Authorisation Pending), 291(Authorised and Active), 292 (Authorised and Inactive), 293(Deleted)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "290, 291, 292, 293")
    public abstract long idLifecycle();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional custom sort information for the frontend to allow the display order of elements being controlled by the backend",
		allowEmptyValue = true,
		required = false)	        
    public abstract Long sortColumn();
}

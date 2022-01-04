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
 * Representing a type for References. A reference type can be considered as the name of a pick list.
 * Maintained by TOMS.
 * @author jwaechter
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceTypeTo.class)
@JsonDeserialize(as = ImmutableReferenceTypeTo.class)
@JsonRootName (value = "referenceType")
@ApiModel(value = "ReferenceType", description = "The TO representation of the type of a reference")
@Value.Style (jdkOnly = true)
public abstract class ReferenceTypeTo {
	@ApiModelProperty(value = "The order management system internal unique ID for the reference type",
			allowEmptyValue = false,
			required = true)
	public abstract long id();

    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The optional name of the reference type",
		allowEmptyValue = true,
		required = false)
    public abstract String name();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional custom sort information for the frontend to allow the display order of elements being controlled by the backend",
		allowEmptyValue = true,
		required = false)	    
    public abstract Long sortColumn();
}

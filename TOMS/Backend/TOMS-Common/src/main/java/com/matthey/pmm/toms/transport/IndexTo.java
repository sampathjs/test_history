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
 * Index setup for two currencies (either metal or normal currency).
 * The index consists of a reference to the index name and
 * references to the two currencies the index is being defined for. 
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableIndexTo.class)
@JsonDeserialize(as = ImmutableIndexTo.class)
@JsonRootName (value = "index")
@ApiModel(value = "Index", description = "An Index consisting of a name and two currencies.")
@Value.Style (jdkOnly = true)
public abstract class IndexTo {
	@ApiModelProperty(value = "The Endur unique ID for an index.",
			allowEmptyValue = false,
			required = true)
	public abstract long id();
   
    @Auxiliary
	@ApiModelProperty(value = "The ID of the name of the index. The ID is instance of a Reference having ReferenceType #11 (Index Name)",
			allowEmptyValue = false,
			required = true)
    public abstract long idIndexName();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the name of the index. The ID is instance of a Reference having ReferenceType #9 (Metal) or ReferenceRype #10 (Currency)",
		allowEmptyValue = false,
		required = true)    
    public abstract long idCurrencyOneName();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the name of the index. The ID is instance of a Reference having ReferenceType #9 (Metal) or ReferenceRype #10 (Currency)",
		allowEmptyValue = false,
		required = true)    
    public abstract long idCurrencyTwoName();

    @Auxiliary
	@ApiModelProperty(value = "The status of the file (deleted, or valid). Allowed values are of reference type #35 (Lifecycle Status): 290(Authorisation Pending), 291(Authorised and Active), 292 (Authorised and Inactive), 293(Deleted)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "290, 291, 292, 293")
    public abstract long idLifecycle();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional value to designate a display order between the different instances of Index",
		allowEmptyValue = false,
		required = true)
    public abstract Long sortColumn();

	@Override
	public String toString() {
		return "IndexTo [id=" + id() + ", idIndexName=" + idIndexName() + ", idCurrencyOneName="
				+ idCurrencyOneName() + ", idCurrencyTwoName=" + idCurrencyTwoName() + ", idLifecycle="
				+ idLifecycle() + "]";
	} 
}

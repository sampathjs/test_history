package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
 * Party entity, in context of TOMS usually a counterparty having an ID, a name and a type.
 * Maintained by Endur
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutablePartyTo.class)
@JsonDeserialize(as = ImmutablePartyTo.class)
@JsonRootName (value = "party")
@ApiModel(value = "Party", description = "The TO representation of a Party (Internal and External Business Units and Legal Entities)")
public abstract class PartyTo {
	/**
	 * Endur side ID
	 * @return
	 */
	@ApiModelProperty(value = "The ID used by both the Order Management System and Endur",
			allowEmptyValue = false,
			required = true)		
    public abstract long id(); 
   
    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The name of the party. Current maximal supported length is 512 characters",
		allowEmptyValue = false,
		required = true)	    
    public abstract String name();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the party type. The IDs are Reference IDs of ReferenceType #1 (Party Type):"
			+  " 1(Internal Business Unit), 2(External Business Unit), 19 (Internal Legal Entity), 20 (External Legal Entity)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "1, 2, 19, 20")       
    public abstract long typeId();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the parent legal entity of this party. This field has to be empty in case of legal entities."
			+ " The reference party has to have typeId=19 or typeId=20 (internal and external legal entity)",
			allowEmptyValue = true,
			required = false,
			allowableValues = "1, 2, 19, 20")       
    public abstract Long idLegalEntity();
    
    @Auxiliary
	@ApiModelProperty(value = "The status of the comment (deleted, or valid). Allowed values are of reference type #35 (Lifecycle Status): 290(Authorisation Pending), 291(Authorised and Active), 292 (Authorised and Inactive), 293(Deleted)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "290, 291, 292, 293")    
    public abstract long idLifecycle();
    
    @Auxiliary
	@ApiModelProperty(value = "Optional custom sort information for the frontend to allow the display order of elements being controlled by the backend",
		allowEmptyValue = true,
		required = false)	        
    public abstract long sortColumn();

}

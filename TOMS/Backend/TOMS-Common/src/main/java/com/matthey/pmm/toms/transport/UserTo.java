package com.matthey.pmm.toms.transport;

import java.util.List;

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
 * Party entity having an ID, a name and a type.
 * Maintained by Endur.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableUserTo.class)
@JsonDeserialize(as = ImmutableUserTo.class)
@JsonRootName (value = "user")
@ApiModel(value = "User", description = "The TO representation of an user of the order management system")
@Value.Style (jdkOnly = true)
public abstract class UserTo {
	/**
	 * Endur side ID.
	 * @return
	 */
	@ApiModelProperty(value = "The ID shared by the Order Management System and Endur",
			allowEmptyValue = false,
			required = true)	
    public abstract long id();
   
    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The optional email address of the user",
		allowEmptyValue = true,
		required = false)	
    public abstract String email();

    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The optional first name of the user",
		allowEmptyValue = true,
		required = false)	
    public abstract String firstName();

    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The optional last name of the user",
		allowEmptyValue = true,
		required = false)	
    public abstract String lastName();

	@ApiModelProperty(value = "The status of the file (deleted, or valid). Allowed values are of reference type #35 (Lifecycle Status): 290(Authorisation Pending), 291(Authorised and Active), 292 (Authorised and Inactive), 293(Deleted)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "290, 291, 292, 293")
    public abstract long idLifecycleStatus();
    
    @Auxiliary
	@ApiModelProperty(value = "The role of the user. Allowed values are of reference type #3 (User Role):"
		+ "3(PMM Front Office User), 4 (PMM Trader), 5 (Admin), 6 (Technical or Service User),"
		+ "294 (PMM Support), 295 (External User)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "range[3,6], 294, 295")
    public abstract long roleId();
    
    @Auxiliary
	@ApiModelProperty(value = "The party IDs of the external business unit the user can trade with.. ",
		allowEmptyValue = true,
		required = false)
    public abstract List<Long> tradeableCounterPartyIds();
    
    @Auxiliary
	@ApiModelProperty(value = "The party IDs of the internal business unit this user can trade with. " + 
			"Allowed values: 20001 (JM PMM US), 20006 (JM PMM UK), 20007 (JM PMM HK), 20008 (JM PM LTD), 20755 (JM PMM CN)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "20001, 20006, 20007, 20008, 20755")
    public abstract List<Long> tradeableInternalPartyIds();
    
    @Auxiliary
	@ApiModelProperty(value = "The IDs of the internal portfolios the user can trade with. The IDs are Reference IDs of ReferenceType #20 (Internal Portfolio)."
			+ "The portfolio list has to match the selected internal party list.",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[118,152], range[296,339]")    
    public abstract List<Long> tradeablePortfolioIds();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The party ID of the default internal business unit of this user. " + 
			"Allowed values: 20001 (JM PMM US), 20006 (JM PMM UK), 20007 (JM PMM HK), 20008 (JM PM LTD), 20755 (JM PMM CN)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "20001, 20006, 20007, 20008, 20755")    
    public abstract Long idDefaultInternalBu();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the default internal portfolio of the user. The IDs are Reference IDs of ReferenceType #20 (Internal Portfolio)."
			+ "The portfolio has to match the selected idDefaultInternalBu and must be element of tradeablePortfolioIds.",
			allowEmptyValue = true,
			required = false,
			allowableValues = "range[118,152], range[296,339]")    
    public abstract Long idDefaultInternalPortfolio();

	@Override
	public String toString() {
		return "UserTo [id=" + id() + ", email=" + email() + ", firstName=" + firstName() + ", lastName="
				+ lastName() + ", idLifecycleStatus=" + idLifecycleStatus() + "]";
	}
}
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
@JsonSerialize(as = ImmutableCreditCheckTo.class)
@JsonDeserialize(as = ImmutableCreditCheckTo.class)
@JsonRootName(value = "orderCreditCheck")
@ApiModel(value = "CreditCheck", description = "Object containing the data for a single request to retrieve a credit check from Endur and the credit check outcome")
public abstract class CreditCheckTo {
	@ApiModelProperty(value = "The order management system internal unique ID for a single credit check request.",
			allowEmptyValue = false,
			required = true)
    public abstract long id(); 
   
	@ApiModelProperty(value = "The ID of the party (business unit) the credit check is executed for.",
			allowEmptyValue = false,
			required = true)
    @Auxiliary
    public abstract long idParty();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the party having ID idParty. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract String displayStringParty();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "After a credit check has completed successfully on a technical level (idCreditCheckRunStatus = #103 (Completed)) this value contains the retrieved credit limit",
		allowEmptyValue = true,
		required = false)
    public abstract Double creditLimit();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "After a credit check has completed successfully on a technical level (idCreditCheckRunStatus = #103 (Completed)) this value contains the retrieved credit limit",
		allowEmptyValue = true,		
		required = false)
    public abstract Double currentUtilization();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "After a credit check has completed on a technical level (idCreditCheckRunStatus = #103 (Completed) or #104 (Failed)) this value contains the timestamp the credit check was run",
		allowEmptyValue = true,		
		required = false)
    public abstract String runDateTime();

    @Auxiliary
	@ApiModelProperty(value = "Tracks the technical status of the credit check request. IDs are instances Reference having ReferenceType #14 (Credit Check Run Status)  : 101(Open), 103(Completed), 104(Failed)",
		allowEmptyValue = false,		
		required = true,
		allowableValues = "101, 103, 104")
    public abstract long idCreditCheckRunStatus();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the technical status of the credit check request. Provided by the backend, but it is not consumed by the backend",
		allowEmptyValue = true,		
		required = false)
    public abstract String displayStringCreditCheckRunStatus();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Tracks the outcome of the credit check from a business perspective. IDs are instances of Reference having ReferenceType #19 (Credit Check Outcome):  114(Passed), 115(AR Failed), 116(Failed)",
		allowEmptyValue = true,		
		required = false,
		allowableValues = "114, 115, 116")
    public abstract Long idCreditCheckOutcome();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the outcome of the credit check request. Provided by the backend, but it is not consumed by the backend",
		allowEmptyValue = true,		
		required = false)
    public abstract String displayStringCreditCheckOutcome();    
}

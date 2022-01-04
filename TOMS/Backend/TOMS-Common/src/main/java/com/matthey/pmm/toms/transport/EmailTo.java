package com.matthey.pmm.toms.transport;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
 * Email with the support for all generic email attributes plus additional
 * metadata and an email workflow support.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableEmailTo.class)
@JsonDeserialize(as = ImmutableEmailTo.class)
@JsonRootName (value = "email")
@ApiModel(value = "Email", description = "A request to send out an email.")
@Value.Style (jdkOnly = true)
public abstract class EmailTo {  
	public static final List<String> UNMODIFIABLE_ATTRBIUTES =
		Arrays.asList("idSendAs", "subject", "body", "toList", "ccList", "bccList", "attachments", "createdAt", "idCreatedByUser", "associatedOrderIds");
	
	@ApiModelProperty(value = "The order management system internal unique ID for the request to send out an email.",
			allowEmptyValue = false,
			required = true)
	public abstract long id();   

    @Auxiliary
	@ApiModelProperty(value = "The ID of the user that is sending out the email. ",
			allowEmptyValue = false,
			required = true)
    public abstract long idSendAs();
    
    @Auxiliary
	@ApiModelProperty(value = "The plain text of the subject the email is to be send out. Mandatory, no empty subject allowed.",
		allowEmptyValue = false,
		required = true)
    public abstract String subject();
    
    @Auxiliary
	@ApiModelProperty(value = "Optional Body text for the email. The body text should be HTML.",
		allowEmptyValue = true,
		required = true)
    public abstract String body();
    
    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "Optional List of direct recipients of the email. Values have to be email addresses. Syntax of the provided values is being validated.",
		allowEmptyValue = true,
		required = false)
    public abstract Set<String> toList();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional List of copied recipients of the email. Values have to be email addresses. Syntax of the provided values is being validated.",
		allowEmptyValue = true,
		required = false)
    public abstract Set<String> ccList();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional List of blind copied recipients of the email. Values have to be email addresses. Syntax of the provided values is being validated.",
		allowEmptyValue = true,
		required = false)
    public abstract Set<String> bccList();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional List of blind copied recipients of the email. Values have to be email addresses. Syntax of the provided values is being validated.",
		allowEmptyValue = true,
		required = false)
    public abstract Set<Long> attachments();
    
    @Auxiliary
	@ApiModelProperty(value = "The status of the email requests. Values are reference IDs of reference type #32 (Email Status): 276(Submitted), 277(Sending), 278(Success), 279(Failed)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "276, 277, 278, 279")
    public abstract long idEmailStatus();
    
    @Auxiliary
    @Nullable    
	@ApiModelProperty(value = "The error message in case the email was not sent correctly. Populated in idEmailStatus #279 (Failed) only by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract String errorMessage();
	
    @Auxiliary
	@ApiModelProperty(value = "The retry count. Integer value >= 0",
		allowEmptyValue = false,
		required = true)
    public abstract int retryCount();
	
    @Auxiliary
	@ApiModelProperty(value = "Timestamp of the creation time of the request to send out an email, assigned by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract String createdAt();
	
    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has created the request to send out an email, assigned by the backend.",
		allowEmptyValue = false,
		required = true)    
    public abstract long idCreatedByUser();
	
    @Auxiliary
	@ApiModelProperty(value = "Timestamp of the last update of the request to send out an email, assigned by the backend.",
		allowEmptyValue = false,
		required = true)    
    public abstract String lastUpdate();
	
    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has updated the request to send out an email, assigned by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract long idUpdatedByUser();
	
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Optional list of IDs of orders that are covered by the email being sent out.",
		allowEmptyValue = false,
		required = true)
    public abstract Set<Long> associatedOrderIds();
}

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
 * Data from (partially) filling a limit or reference order for TOMS (Transfer Object)
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableFillTo.class)
@JsonDeserialize(as = ImmutableFillTo.class)
@JsonRootName (value = "orderFill")
@ApiModel(value = "Fill", description = "Request to fill an order. The requests includes a deal to be booked on Endur side. The request is processed over several status to cover the risk the Endur side deal booking is going to fail.")
@Value.Style (jdkOnly = true)
public abstract class FillTo {
	@ApiModelProperty(value = "The order management system internal unique ID for the request to fill an order.",
			allowEmptyValue = false,
			required = true)	
    public abstract long id();   
    
    @Auxiliary
	@ApiModelProperty(value = "The quantity of the order to be filled.",
			allowEmptyValue = false,
			required = true)
    public abstract double fillQuantity();

    @Auxiliary
	@ApiModelProperty(value = "The price the order is being filled for.",
		allowEmptyValue = false,
		required = true)
    public abstract double fillPrice();
    
    /*
     * Endur side Trade ID
     */
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "ID of the trade the order is being filled with. The value is null if idFillStatus is NOT #287 (Completed)",
		allowEmptyValue = true,
		required = false)
    public abstract Long idTrade();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has filled the order.",
		allowEmptyValue = false,
		required = true)
    public abstract long idTrader();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the user having ID idTrader. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
    public abstract String displayStringTrader();

    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has last updated the request to fill the order, assigned by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract long idUpdatedBy();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the user having ID idUpdatedBy. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
    public abstract String displayStringUpdatedBy();    
    
    @Auxiliary
	@ApiModelProperty(value = "Timestamp of the last update of the request to send out an email, assigned by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract String lastUpdateDateTime();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the status of the fill request. The IDs are Reference IDs of ReferenceType #34 (Fill Status): 286(Open), 287(Completed), 288(Failed)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "286, 287, 288")    
    public abstract long idFillStatus();    
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the fill status as provided in idFillStatus. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
    public abstract String displayStringFillStatus();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "Error message in case a problem happend when booking the trade. The value is null if idFillStatus is NOT #288 (Failed)",
		allowEmptyValue = true,
		required = false)
    public abstract String errorMessage();
}

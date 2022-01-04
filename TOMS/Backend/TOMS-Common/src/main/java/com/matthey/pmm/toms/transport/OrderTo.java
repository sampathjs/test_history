package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Abstract base class for Limit Orders and Reference Orders
 * @author jwaechter
 * @version 1.0
 */
@JsonSubTypes({
  @JsonSubTypes.Type(value=LimitOrderTo.class, name = "LimitOrder"),
  @JsonSubTypes.Type(value=ReferenceOrderTo.class, name = "ReferenceOrder")
})
@ApiModel(value = "Order", description = "The abtract TO representation of an Order Object (The attributes are shared both on Limit and Reference Order).",
	subTypes = {LimitOrderTo.class, ReferenceOrderTo.class}, discriminator = "idOrderType")
@Immutable
@JsonSerialize(as = ImmutableOrderTo.class)
@JsonDeserialize(as = ImmutableOrderTo.class)
public abstract class OrderTo {	
	/**
	 * TOMS maintained ID 
	 */	
	@ApiModelProperty(value = "The order management system internal unique ID for a Limit or Reference Order. Forms primary key together with version.",
			allowEmptyValue = false,
			required = true)
	public abstract long id();

	@ApiModelProperty(value = "The order management system version of an Order",
			allowEmptyValue = false,
			required = true)
	public abstract int version();
	

	@Auxiliary
	@ApiModelProperty(value = "The party ID of the internal business unit of this order. " + 
			"Allowed values: 20001 (JM PMM US), 20006 (JM PMM UK), 20007 (JM PMM HK), 20008 (JM PM LTD), 20755 (JM PMM CN)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "20001, 20006, 20007, 20008, 20755")
	public abstract long idInternalBu();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the internal busines unit as provided in idInternalBu. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringInternalBu(); 
    
    @Auxiliary
	@Nullable
	@ApiModelProperty(value = "The party ID of the external business unit of this order. ",
		allowEmptyValue = true,
		required = false)
    public abstract Long idExternalBu();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the external business unit as provided in idExternalBu. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)		
	public abstract String displayStringExternalBu(); 

    @Auxiliary
	@Nullable
	@ApiModelProperty(value = "The party ID of the internal legal entity of this order. The value is being derived from the selected internal business unit. "
			+ "It is not allowed to provide a legal entity ID that is not belonging to the selected business unit." + 
			"Allowed values: 20002 (JM INC, LE for JM PMM US), 20003 (JM PACIFIC LTD, LE for JM PMM HK), "
			+ "20004 (JM PLC, LE for JM PMM UK), 20005 (JM PRECIOUS METALS LTD, LE for JM PM LTD), 20756 (JM (CN) CATALYST CO - LE, LE for JM PMM CN) ",
		allowEmptyValue = true,
		required = false,
		allowableValues = "20002, 20003, 20004, 20005, 20756")
    public abstract Long idInternalLe();
    
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the internal legal entity as provided in idInternalLe. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)			
	public abstract String displayStringInternalLe(); 

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The party ID of the external legal entity of this order. The value is being derived from the selected external business unit. "
			+ "It is not allowed to provide a legal entity ID that is not belonging to the selected business unit.",
		allowEmptyValue = true,
		required = false)
    public abstract Long idExternalLe();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the external legal entity as provided in idExternalLe. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringExternalLe();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The ID of the internal portfolio of the order. The IDs are Reference IDs of ReferenceType #20 (Internal Portfolio)."
		+ "The portfolio has to match the selected internal party.",
		allowEmptyValue = false,
		required = true,
		allowableValues = "range[118,152], range[296,339]")
    public abstract Long idIntPortfolio();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the internal portfolio as provided in idIntPortfolio. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringIntPortfolio();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The ID of the internal portfolio of the order. The IDs are Reference IDs of ReferenceType #20 (Internal Portfolio)."
		+ " The external portfolio is being populated in case the external counterparty is an internal JM party only as JM does not maintain the portfolios for their clients."
		+ " The portfolio has to match the selected external party",
		allowEmptyValue = false,
		required = true,
		allowableValues = "range[118,152], range[296,339]")	
    public abstract Long idExtPortfolio();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the external portfolio as provided in idExtPortfolio. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringExtPortfolio();

	@Auxiliary
	@ApiModelProperty(value = "The ID of the flag indicating whether the order is buying or selling. The IDs are Reference IDs of ReferenceType #5 (Buy/Sell): 15(Buy), 16(Sell)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "15, 16")
	public abstract long idBuySell();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the buy/sell flag as provided in idBuySell. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringBuySell();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the base currency. The IDs are Reference IDs of ReferenceType #9 (Metal): 34(XRU), 35(XOS), 36(XIR), 37(XAG), 38(XPT), 39(XRH), 40 (XAU), 41 (XPD)"
		+ " or of ReferenceType #10 (Currency): 42(USD), 43(EUR), 44(CNY), 45(ZAR), 46(GBP)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "range[34, 46]")
    public abstract long idBaseCurrency();
    
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the base currency as provided in idBaseCurrency. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringBaseCurrency();
    
    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The quantity of the base currency (idBaseCurrency) in the provided base quantity unit (idBaseQuantityUnit)",
		allowEmptyValue = true,
		required = false)
    public abstract Double baseQuantity();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the base quantity. The IDs are Reference IDs of ReferenceType #8 (Quantity Unit):"
			+ " 28(TOz), 29(MT), 30(gms), 31(kgs), 32(lbs), 33(mgs)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[28, 33]")    
    public abstract Long idBaseQuantityUnit();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the base quantity unit as provided in idBaseQuantityUnit. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringBaseQuantityUnit();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the term currency. The IDs are Reference IDs of ReferenceType #9 (Metal): 34(XRU), 35(XOS), 36(XIR), 37(XAG), 38(XPT), 39(XRH), 40 (XAU), 41 (XPD)"
			+ " or of ReferenceType #10 (Currency): 42(USD), 43(EUR), 44(CNY), 45(ZAR), 46(GBP)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[34, 46]")
    public abstract Long idTermCurrency();
    
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the term currency as provided in idTermCurrency. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringTermCurrency();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "A free text defined additional piece of information about the order, limited to 32 characters.",
		allowEmptyValue = true,
		required = false)
    public abstract String reference();    
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the form of the metal. The IDs are Reference IDs of ReferenceType #22 (Form): "
	+ 		" 164(Ingot), 165(Sponge), 166(Grain), 167(None)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[164, 167]")
    public abstract Long idMetalForm();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the metal form as provided in idMetalForm. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringMetalForm();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the form of the metal. The IDs are Reference IDs of ReferenceType #23 (Location):"
		+  " 168(Germiston SA), 169(Hong Kong), 170(London Plate), 171(None), 172 (Royston), 173 (Tanaka Japan)"
		+  ", 174(Umicore Belgium), 175(Vale), 176 (Valley Forge), 177 (Zurich), 178 (Shanghai), 179 (Brandenberger)"
		+  ", 180(LME), 181(Brinks), 182(CNT), 183 (Umicore Germany)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "range[168, 183]")
    public abstract Long idMetalLocation();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the metal location as provided in idMetalLocation. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringMetalLocation();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the status the order is currently in. Referenced IDs are of type OrderStatus",
		allowEmptyValue = false,
		required = true)
    public abstract Long idOrderStatus();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the order status as provided in idOrderStatus. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringOrderStatus();

    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The IDs of the credit checks requested or finished for this order.",
		allowEmptyValue = true,
		required = false)
    public abstract List<Long> creditChecksIds();
    
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
	@ApiModelProperty(value = "The fill percentage as a number between 0 and 1. The value is calculated by the backend based on the order quantity and the existing fills."
		+ ". It has to be provided by the frontend when submitting order data, but those values are going to get ignored.",
		allowEmptyValue = true,
		required = false)
    public abstract double fillPercentage();
    
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The ID of the ticker. The IDs are Reference IDs of ReferenceType #30 (Ticker):"
			+  " 234 (XAG/CNY), 235 (XAG/EUR), 236 (XAG/GBP), 237 (XAG/USD), 238 (XAU/CNY), 239 (XAU/EUR)"
			+  ", 240 (XAU/GBP), 241 (XAU/USD), 242 (XIR/CNY), 243 (XIR/EUR), 244 (XIR/GBP), 245 (XIR/USD)"
			+  ", 246 (XIR/ZAR), 247(XOS/CNY), 248(XOS/EUR), 249 (XOS/GBP), 250 (XOS/USD), 251 (XPD/CNY)"
			+  ", 252 (XPD/EUR), 253(XPD/GBP), 254(XPD/USD), 255 (XPD/ZAR), 256 (XPT/CNY), 257 (XPT/EUR)"
			+  ", 258 (XPT/GBP), 259(XPT/USD), 260(XPT/ZAR), 261 (XRH/CNY), 262 (XRH/EUR), 263 (XRH/GBP)"
			+  ", 264 (XRH/USD), 265(XRH/ZAR), 266(XRU/CNY), 267 (XRU/EUR), 268 (XRU/GBP), 269 (XRU/USD), 270 (XRU/ZAR)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[234, 270]")
	public abstract Long idTicker(); 

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the ticker as provided in idTicker. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringTicker();
	
	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the contract type. The IDs are Reference IDs of ReferenceType #27 (Contract Type Reference Order):"
			+  " 224(Average), 225(Fixing), "
			+  " or of ReferenceType #28 (Contract Type Limit Order): "
			+  " 226(Relative), 227(Fixed)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[224, 227]")    
	public abstract Long idContractType();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the contract type as provided in idContractType. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringContractType();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The ID of the order type. The IDs are Reference IDs of ReferenceType #2 (Order Type):"
			+  " 13(Limit Order), 14(Reference Order), ",
			allowEmptyValue = false,
			required = true,
			allowableValues = "13, 14")
	public abstract Long idOrderType();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the order type as provided in idOrderType. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringOrderType();
	
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The list of order comment IDs once created for the order",
		allowEmptyValue = true,
		required = false)
    public abstract List<Long> orderCommentIds();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The list of fill IDs created for the order",
		allowEmptyValue = true,
		required = false)
    public abstract List<Long> fillIds();
}

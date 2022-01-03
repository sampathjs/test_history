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
 * A Reference Order for TOMS.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableReferenceOrderLegTo.class)
@JsonDeserialize(as = ImmutableReferenceOrderLegTo.class)
@JsonRootName (value = "referenceOrderLeg")
@ApiModel(value = "ReferenceOrderLeg", description = "The TO representation of the leg of a reference order.")
public abstract class ReferenceOrderLegTo {
	/**
	 * TOMS maintained ID 
	 */	
	@ApiModelProperty(value = "The order management system internal unique ID for the leg of a reference order",
			allowEmptyValue = false,
			required = true)	
	public abstract long id();
	
	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The notional for the leg of the reference order.",
		allowEmptyValue = true,
		required = false)
	public abstract Double notional();
	
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The Fixing Start Date (in YYYY-MM-DD format without timestamp) of the reference order leg.",
		allowEmptyValue = true,
		required = false)
    public abstract String fixingStartDate();
	
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The Fixing End Date (in YYYY-MM-DD format without timestamp) of the reference order leg.",
		allowEmptyValue = true,
		required = false)
    public abstract String fixingEndDate();
    
    @Auxiliary
	@ApiModelProperty(value = "The Payment Date (in YYYY-MM-DD format without timestamp) of the reference order leg.",
		allowEmptyValue = true,
		required = false)
	public abstract String paymentDate();
    
    @Nullable
    @Auxiliary
	@ApiModelProperty(value = "The ID of the leg settle currency. The IDs are Reference IDs of ReferenceType #9 (Metal): 34(XRU), 35(XOS), 36(XIR), 37(XAG), 38(XPT), 39(XRH), 40 (XAU), 41 (XPD)"
			+ " or of ReferenceType #10 (Currency): 42(USD), 43(EUR), 44(CNY), 45(ZAR), 46(GBP)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "[34, 46]")
    public abstract Long idSettleCurrency();
    
	@Auxiliary
    @Nullable
	@ApiModelProperty(value = "The name of the settle currency as provided in idSettleCurrency. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringSettleCurrency();
    
    @Auxiliary
	@ApiModelProperty(value = "The ID of the leg reference source."
			+ " The IDs are Reference IDs of ReferenceType #26 (Ref Source):"
			+ " 190(None), 191(Manual), 192(BLOOMBERG), 193(LBMA), 194(LPPM), 195(LIBOR-BBA), 196 (LBMA PM)"
			+ " 197(LBMA AM), 198(JM NY Opening), 199(COMEX), 200(JM London Opening), 201(JM HK Opening), 202(JM HK Closing), 203(ECB)"
			+ " 204(Custom), 205(LME AM), 206(LME PM), 207(Comdaq), 208(NY MW High), 209(NY MW Low), 210(LBMA Silver)"
			+ " 211(LME Base), 212(Citibank Forward), 213(Physical), 214(Citibank 1500), 215(Impala Rate), 216(NY MW Mid), 217(FNB)"
			+ " 218(BFIX 1500), 219(BFIX Forward), 220(BFIX 1400), 221(BOC), 222(JMLO BFIX 1400), 223(JMLO BFIX 1500)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "[190, 223]")
	public abstract long idRefSource();

    @Nullable
	@Auxiliary
	@ApiModelProperty(value = "The name of the Reference Source as provided in idRefSource. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringRefSource();
    
    @Auxiliary
    @Nullable
	@ApiModelProperty(value = "The ID of the leg reference source. Note the FX Index Ref Source is subject to validation by the TickerFxRefSourceRule."
			+ " The IDs are Reference IDs of ReferenceType #26 (Ref Source):"
			+ " 190(None), 191(Manual), 192(BLOOMBERG), 193(LBMA), 194(LPPM), 195(LIBOR-BBA), 196 (LBMA PM)"
			+ " 197(LBMA AM), 198(JM NY Opening), 199(COMEX), 200(JM London Opening), 201(JM HK Opening), 202(JM HK Closing), 203(ECB)"
			+ " 204(Custom), 205(LME AM), 206(LME PM), 207(Comdaq), 208(NY MW High), 209(NY MW Low), 210(LBMA Silver)"
			+ " 211(LME Base), 212(Citibank Forward), 213(Physical), 214(Citibank 1500), 215(Impala Rate), 216(NY MW Mid), 217(FNB)"
			+ " 218(BFIX 1500), 219(BFIX Forward), 220(BFIX 1400), 221(BOC), 222(JMLO BFIX 1400), 223(JMLO BFIX 1500)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "[190, 223]")
	public abstract Long idFxIndexRefSource();
    
    @Nullable
	@Auxiliary
	@ApiModelProperty(value = "The name of the FX Index Reference Source as provided in idFxIndexRefSource. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringFxIndexRefSource();    
}

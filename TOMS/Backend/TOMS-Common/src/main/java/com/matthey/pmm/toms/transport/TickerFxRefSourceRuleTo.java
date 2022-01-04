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
 * Contains a row in the Index <-> Ticker <-> Ref Source rule table.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableTickerFxRefSourceRuleTo.class)
@JsonDeserialize(as = ImmutableTickerFxRefSourceRuleTo.class)
@JsonRootName (value = "ticker_ref_source_rule")
@ApiModel(value = "Ticker <-> FX Ref Source Rule", description = "The TO representation of the type of Validation rule from ticker to index, term currency and FX Reference Source")
public abstract class TickerFxRefSourceRuleTo {
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
	public abstract long idTicker();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the ticker as provided in idTicker. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringTicker();

	@ApiModelProperty(value = "The ID of the index. The IDs are Reference IDs of ReferenceType #11 (Index Name)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[49, 96], range[340, 376]")
	public abstract long idIndex();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the index as provided in idIndex. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringIndex();
	
	@ApiModelProperty(value = "The ID of the base currency. The IDs are Reference IDs of ReferenceType #9 (Metal): 34(XRU), 35(XOS), 36(XIR), 37(XAG), 38(XPT), 39(XRH), 40 (XAU), 41 (XPD)"
			+ " or of ReferenceType #10 (Currency): 42(USD), 43(EUR), 44(CNY), 45(ZAR), 46(GBP)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[34, 46]")
	public abstract long idTermCurrency();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the term currency as provided in idTermCurrency. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringTermCurrency();
	
	@ApiModelProperty(value = "The ID of the leg reference source."
			+ " The IDs are Reference IDs of ReferenceType #26 (Ref Source):"
			+ " 190(None), 191(Manual), 192(BLOOMBERG), 193(LBMA), 194(LPPM), 195(LIBOR-BBA), 196 (LBMA PM)"
			+ " 197(LBMA AM), 198(JM NY Opening), 199(COMEX), 200(JM London Opening), 201(JM HK Opening), 202(JM HK Closing), 203(ECB)"
			+ " 204(Custom), 205(LME AM), 206(LME PM), 207(Comdaq), 208(NY MW High), 209(NY MW Low), 210(LBMA Silver)"
			+ " 211(LME Base), 212(Citibank Forward), 213(Physical), 214(Citibank 1500), 215(Impala Rate), 216(NY MW Mid), 217(FNB)"
			+ " 218(BFIX 1500), 219(BFIX Forward), 220(BFIX 1400), 221(BOC), 222(JMLO BFIX 1400), 223(JMLO BFIX 1500)",
			allowEmptyValue = false,
			required = true,
			allowableValues = "range[190, 223]")
	public abstract long idRefSource();

	@Auxiliary
	@Nullable
	@ApiModelProperty(value = "The name of the ref source as provided in idRefSource. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringRefSource();	
}

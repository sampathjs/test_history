package com.matthey.pmm.toms.transport;

import org.immutables.value.Value;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Contains a row in the Portfolio <-> Party <-> Ticker <-> Index mapping table.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableTickerPortfolioRuleTo.class)
@JsonDeserialize(as = ImmutableTickerPortfolioRuleTo.class)
@JsonRootName (value = "ticker_portfolio_rule")
@ApiModel(value = "Ticker <-> Portfolio Rule", description = "The TO representation of the type of Validation rule from ticker to porfolio")
@Value.Style (jdkOnly = true)
public abstract class TickerPortfolioRuleTo {
	@ApiModelProperty(value = "The ID of the internal portfolio of the order. The IDs are Reference IDs of ReferenceType #20 (Internal Portfolio).",
			allowEmptyValue = false,
			required = true,
			allowableValues = "[118,152], [296,339]")
	public abstract long idPortfolio();
	
	@Auxiliary
	@Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the portfolio as provided in idPortfolio. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringPortfolio();
	
	
	@ApiModelProperty(value = "The ID of the internal party of the order. The IDs are IDs of instances of Parties.",
			allowEmptyValue = false,
			required = true)
	public abstract long idParty();

	@Auxiliary
	@Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the party as provided in idParty. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)	
	public abstract String displayStringParty();

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
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)	
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
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the index as provided in idIndex. Provided by the backend, but it is not consumed by the backend.",
		allowEmptyValue = true,
		required = false)
	public abstract String displayStringIndex();
}

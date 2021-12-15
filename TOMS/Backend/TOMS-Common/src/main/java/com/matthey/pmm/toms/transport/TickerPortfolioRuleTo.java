package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Contains a row in the Portfolio <-> Party <-> Ticker <-> Index mapping table.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableTickerPortfolioRuleTo.class)
@JsonDeserialize(as = ImmutableTickerPortfolioRuleTo.class)
@JsonRootName (value = "ticker_portfolio_rule")
public abstract class TickerPortfolioRuleTo {
	public abstract long idPortfolio();
	
	@Auxiliary
	@Nullable
	public abstract String displayStringPortfolio();
	
	public abstract long idParty();

	@Auxiliary
	@Nullable
	public abstract String displayStringParty();

	public abstract long idTicker();

	@Auxiliary
	@Nullable
	public abstract String displayStringTicker();

	public abstract long idIndex();

	@Auxiliary
	@Nullable
	public abstract String displayStringIndex();
}

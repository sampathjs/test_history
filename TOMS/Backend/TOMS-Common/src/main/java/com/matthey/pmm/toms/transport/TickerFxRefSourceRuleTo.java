package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Contains a row in the Index <-> Ticker <-> Ref Source rule table.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableTickerFxRefSourceRuleTo.class)
@JsonDeserialize(as = ImmutableTickerFxRefSourceRuleTo.class)
@JsonRootName (value = "ticker_ref_source_rule")
public abstract class TickerFxRefSourceRuleTo {
	public abstract long idTicker();

	@Auxiliary
	@Nullable
	public abstract String displayStringTicker();

	public abstract long idIndex();

	@Auxiliary
	@Nullable
	public abstract String displayStringIndex();
	
	public abstract long idTermCurrency();

	@Auxiliary
	@Nullable
	public abstract String displayStringTermCurrency();
	
	public abstract long idRefSource();

	@Auxiliary
	@Nullable
	public abstract String displayStringRefSource();	
}

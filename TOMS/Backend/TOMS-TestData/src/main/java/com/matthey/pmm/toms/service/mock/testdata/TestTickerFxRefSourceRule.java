package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableTickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;

public enum TestTickerFxRefSourceRule {
	RULE_1 (DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_2 (DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_3 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_4 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_5 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_6 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_7 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_8 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_9 (DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_10 (DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_11 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_12 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_13 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_14 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_15 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_16 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1400),
	RULE_17 (DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_18 (DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_19 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_20 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_21 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_22 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_23 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_24 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_FX_USD_ZAR,	DefaultReference.INDEX_NAME_FX_USD_ZAR,	DefaultReference.CCY_ZAR,	DefaultReference.REF_SOURCE_BFIX_1500),
	// the following rules are for technical testing only and are supposed to demonstrate that the population of the FX Ref Source will show a pick
	// list in case of multiple results instead of just populating the field with the single rule left over.
	RULE_50_JW (DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_FX_EUR_USD,	DefaultReference.INDEX_NAME_FX_EUR_USD,	DefaultReference.CCY_EUR,	DefaultReference.REF_SOURCE_BFIX_1500),
	RULE_51_JW (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_FX_GBP_USD,	DefaultReference.INDEX_NAME_FX_GBP_USD,	DefaultReference.CCY_GBP,	DefaultReference.REF_SOURCE_BFIX_1500),
	;
	
	private TickerFxRefSourceRuleTo entity;
	
	private TestTickerFxRefSourceRule (DefaultReference ticker, TestIndex index, DefaultReference indexName, 
			DefaultReference termCurrency, DefaultReference refSource) {
		entity = ImmutableTickerFxRefSourceRuleTo.builder()
				.displayStringIndex(indexName.getEntity().name())
				.displayStringTicker(ticker.getEntity().name())
				.displayStringRefSource(refSource.getEntity().name())
				.displayStringTermCurrency(termCurrency.getEntity().name())
				.idIndex(index.getEntity().id())
				.idTicker(ticker.getEntity().id())
				.idRefSource(refSource.getEntity().id())
				.idTermCurrency(termCurrency.getEntity().id())
				.build();
	}

	public TickerFxRefSourceRuleTo getEntity () {
		return entity;
	}
	
	public void setEntity (TickerFxRefSourceRuleTo entity) {
		this.entity = entity;
	}
	
	
	public static List<TickerFxRefSourceRuleTo> asList () {
		return Arrays.asList(TestTickerFxRefSourceRule.values())
				.stream().map(TestTickerFxRefSourceRule::getEntity).collect(Collectors.toList());
	}

	public static List<TestTickerFxRefSourceRule> asEnumList () {
		return Arrays.asList(TestTickerFxRefSourceRule.values())
				.stream().collect(Collectors.toList());
	}
}

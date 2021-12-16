package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableTickerRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

public enum TestTickerRefSourceRule {
	RULE_49 (DefaultReference.TICKER_XAGEUR,	TestIndex.INDEX_XAG_EUR,	DefaultReference.INDEX_NAME_XAG_EUR,	DefaultReference.REF_SOURCE_LBMA_AM),
	RULE_48 (DefaultReference.TICKER_XAGEUR,	TestIndex.INDEX_XAG_EUR,	DefaultReference.INDEX_NAME_XAG_EUR,	DefaultReference.REF_SOURCE_LBMA_PM),
	RULE_58 (DefaultReference.TICKER_XAGGBP,	TestIndex.INDEX_XAG_GBP,	DefaultReference.INDEX_NAME_XAG_GBP,	DefaultReference.REF_SOURCE_LBMA_AM),
	RULE_57 (DefaultReference.TICKER_XAGGBP,	TestIndex.INDEX_XAG_GBP,	DefaultReference.INDEX_NAME_XAG_GBP,	DefaultReference.REF_SOURCE_LBMA_PM),
	RULE_3 (DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD,	DefaultReference.INDEX_NAME_XAG_USD,	DefaultReference.REF_SOURCE_LBMA_PM),
	RULE_4 (DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD,	DefaultReference.INDEX_NAME_XAG_USD,	DefaultReference.REF_SOURCE_LBMA_AM),
	RULE_50 (DefaultReference.TICKER_XAUEUR,	TestIndex.INDEX_XAU_EUR,	DefaultReference.INDEX_NAME_XAU_EUR,	DefaultReference.REF_SOURCE_LBMA_PM),
	RULE_52 (DefaultReference.TICKER_XAUEUR,	TestIndex.INDEX_XAU_EUR,	DefaultReference.INDEX_NAME_XAU_EUR,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_51 (DefaultReference.TICKER_XAUEUR,	TestIndex.INDEX_XAU_EUR,	DefaultReference.INDEX_NAME_XAU_EUR,	DefaultReference.REF_SOURCE_LBMA_AM),
	RULE_60 (DefaultReference.TICKER_XAUGBP,	TestIndex.INDEX_XAU_GBP,	DefaultReference.INDEX_NAME_XAU_GBP,	DefaultReference.REF_SOURCE_LBMA_AM),
	RULE_61 (DefaultReference.TICKER_XAUGBP,	TestIndex.INDEX_XAU_GBP,	DefaultReference.INDEX_NAME_XAU_GBP,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_59 (DefaultReference.TICKER_XAUGBP,	TestIndex.INDEX_XAU_GBP,	DefaultReference.INDEX_NAME_XAU_GBP,	DefaultReference.REF_SOURCE_LBMA_PM),
	RULE_2 (DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD,	DefaultReference.INDEX_NAME_XAU_USD,	DefaultReference.REF_SOURCE_LBMA_AM),
	RULE_1 (DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD,	DefaultReference.INDEX_NAME_XAU_USD,	DefaultReference.REF_SOURCE_LBMA_PM),
	RULE_36 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_NY_MW_MID),
	RULE_35 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_NY_MW_LOW),
	RULE_29 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_JM_NY_OPENING),
	RULE_33 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_COMDAQ),
	RULE_32 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_JM_HK_CLOSING),
	RULE_31 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_JM_HK_OPENING),
	RULE_30 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_34 (DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD,	DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.REF_SOURCE_NY_MW_HIGH),
	RULE_6 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD,	DefaultReference.INDEX_NAME_XOS_USD,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_8 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD,	DefaultReference.INDEX_NAME_XOS_USD,	DefaultReference.REF_SOURCE_JM_HK_CLOSING),
	RULE_7 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD,	DefaultReference.INDEX_NAME_XOS_USD,	DefaultReference.REF_SOURCE_JM_HK_OPENING),
	RULE_5 (DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD,	DefaultReference.INDEX_NAME_XOS_USD,	DefaultReference.REF_SOURCE_JM_NY_OPENING),
	RULE_55 (DefaultReference.TICKER_XPDEUR,	TestIndex.INDEX_XPD_EUR,	DefaultReference.INDEX_NAME_XPD_EUR,	DefaultReference.REF_SOURCE_LME_PM),
	RULE_54 (DefaultReference.TICKER_XPDEUR,	TestIndex.INDEX_XPD_EUR,	DefaultReference.INDEX_NAME_XPD_EUR,	DefaultReference.REF_SOURCE_LME_AM),
	RULE_53 (DefaultReference.TICKER_XPDEUR,	TestIndex.INDEX_XPD_EUR,	DefaultReference.INDEX_NAME_XPD_EUR,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_64 (DefaultReference.TICKER_XPDGBP,	TestIndex.INDEX_XPD_GBP,	DefaultReference.INDEX_NAME_XPD_GBP,	DefaultReference.REF_SOURCE_LME_PM),
	RULE_62 (DefaultReference.TICKER_XPDGBP,	TestIndex.INDEX_XPD_GBP,	DefaultReference.INDEX_NAME_XPD_GBP,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_63 (DefaultReference.TICKER_XPDGBP,	TestIndex.INDEX_XPD_GBP,	DefaultReference.INDEX_NAME_XPD_GBP,	DefaultReference.REF_SOURCE_LME_AM),
	RULE_21 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD,	DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.REF_SOURCE_LME_AM),
	RULE_17 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD,	DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.REF_SOURCE_JM_NY_OPENING),
	RULE_19 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD,	DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.REF_SOURCE_JM_HK_OPENING),
	RULE_20 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD,	DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.REF_SOURCE_JM_HK_CLOSING),
	RULE_22 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD,	DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.REF_SOURCE_LME_PM),
	RULE_18 (DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD,	DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_45 (DefaultReference.TICKER_XPTEUR,	TestIndex.INDEX_XPT_EUR,	DefaultReference.INDEX_NAME_XPT_EUR,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_46 (DefaultReference.TICKER_XPTEUR,	TestIndex.INDEX_XPT_EUR,	DefaultReference.INDEX_NAME_XPT_EUR,	DefaultReference.REF_SOURCE_LME_AM),
	RULE_47 (DefaultReference.TICKER_XPTEUR,	TestIndex.INDEX_XPT_EUR,	DefaultReference.INDEX_NAME_XPT_EUR,	DefaultReference.REF_SOURCE_LME_PM),
	RULE_65 (DefaultReference.TICKER_XPTGBP,	TestIndex.INDEX_XPT_GBP,	DefaultReference.INDEX_NAME_XPT_GBP,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_66 (DefaultReference.TICKER_XPTGBP,	TestIndex.INDEX_XPT_GBP,	DefaultReference.INDEX_NAME_XPT_GBP,	DefaultReference.REF_SOURCE_LME_AM),
	RULE_67 (DefaultReference.TICKER_XPTGBP,	TestIndex.INDEX_XPT_GBP,	DefaultReference.INDEX_NAME_XPT_GBP,	DefaultReference.REF_SOURCE_LME_PM),
	RULE_23 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD,	DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.REF_SOURCE_JM_NY_OPENING),
	RULE_24 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD,	DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_25 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD,	DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.REF_SOURCE_JM_HK_OPENING),
	RULE_26 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD,	DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.REF_SOURCE_JM_HK_CLOSING),
	RULE_27 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD,	DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.REF_SOURCE_LME_AM),
	RULE_28 (DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD,	DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.REF_SOURCE_LME_PM),
	RULE_56 (DefaultReference.TICKER_XRHEUR,	TestIndex.INDEX_XRH_EUR,	DefaultReference.INDEX_NAME_XRH_EUR,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_68 (DefaultReference.TICKER_XRHGBP,	TestIndex.INDEX_XRH_GBP,	DefaultReference.INDEX_NAME_XRH_GBP,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_43 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_NY_MW_LOW),
	RULE_39 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_JM_HK_OPENING),
	RULE_37 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_JM_NY_OPENING),
	RULE_42 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_NY_MW_HIGH),
	RULE_38 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_40 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_JM_HK_CLOSING),
	RULE_44 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_NY_MW_MID),
	RULE_41 (DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD,	DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.REF_SOURCE_COMDAQ),
	RULE_12 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_JM_HK_CLOSING),
	RULE_14 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_NY_MW_HIGH),
	RULE_13 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_COMDAQ),
	RULE_16 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_NY_MW_MID),
	RULE_11 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_JM_HK_OPENING),
	RULE_10 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_JM_LONDON_OPENING),
	RULE_15 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_NY_MW_LOW),
	RULE_9 (DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD,	DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.REF_SOURCE_JM_NY_OPENING),
	;
	
	private TickerRefSourceRuleTo entity;
	
	private TestTickerRefSourceRule (DefaultReference ticker, TestIndex index, DefaultReference indexName, DefaultReference refSource) {
		entity = ImmutableTickerRefSourceRuleTo.builder()
				.displayStringIndex(indexName.getEntity().name())
				.displayStringTicker(ticker.getEntity().name())
				.displayStringRefSource(refSource.getEntity().name())
				.idIndex(index.getEntity().id())
				.idTicker(ticker.getEntity().id())
				.idRefSource(refSource.getEntity().id())
				.build();
	}

	public TickerRefSourceRuleTo getEntity () {
		return entity;
	}
	
	public void setEntity (TickerRefSourceRuleTo entity) {
		this.entity = entity;
	}
	
	
	public static List<TickerRefSourceRuleTo> asList () {
		return Arrays.asList(TestTickerRefSourceRule.values())
				.stream().map(TestTickerRefSourceRule::getEntity).collect(Collectors.toList());
	}

	public static List<TestTickerRefSourceRule> asEnumList () {
		return Arrays.asList(TestTickerRefSourceRule.values())
				.stream().collect(Collectors.toList());
	}
}

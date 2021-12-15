package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableTickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;

public enum TestTickerPortfolioRule {
	RULE_1 (DefaultReference.PORTFOLIO_US_SILVER,	TestBunit.JM_PMM_US,	DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD, DefaultReference.INDEX_NAME_XAG_USD),
	RULE_2 (DefaultReference.PORTFOLIO_US_GOLD, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD, DefaultReference.INDEX_NAME_XAU_USD),
	RULE_3 (DefaultReference.PORTFOLIO_US_IRIDIUM, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD, DefaultReference.INDEX_NAME_XIR_USD),
	RULE_4 (DefaultReference.PORTFOLIO_US_OSMIUM, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD, DefaultReference.INDEX_NAME_XOS_USD),
	RULE_5 (DefaultReference.PORTFOLIO_US_PALLADIUM, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD, DefaultReference.INDEX_NAME_XPD_USD),
	RULE_6 (DefaultReference.PORTFOLIO_US_PLATINUM, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD, DefaultReference.INDEX_NAME_XPT_USD),
	RULE_7 (DefaultReference.PORTFOLIO_US_RHODIUM, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD, DefaultReference.INDEX_NAME_XRH_USD),
	RULE_8 (DefaultReference.PORTFOLIO_US_RUTHENIUM, TestBunit.JM_PMM_US,	DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD, DefaultReference.INDEX_NAME_XRU_USD),
	RULE_9 (DefaultReference.PORTFOLIO_UK_SILVER, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XAGEUR,	TestIndex.INDEX_XAG_EUR, DefaultReference.INDEX_NAME_XAG_EUR),
	RULE_10 (DefaultReference.PORTFOLIO_UK_SILVER, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XAGGBP,	TestIndex.INDEX_XAG_GBP, DefaultReference.INDEX_NAME_XAG_GBP),
	RULE_11 (DefaultReference.PORTFOLIO_UK_SILVER, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD, DefaultReference.INDEX_NAME_XAG_USD),
	RULE_12 (DefaultReference.PORTFOLIO_UK_GOLD, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XAUEUR,	TestIndex.INDEX_XAU_EUR, DefaultReference.INDEX_NAME_XAU_EUR),
	RULE_13 (DefaultReference.PORTFOLIO_UK_GOLD	, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XAUGBP,	TestIndex.INDEX_XAU_GBP, DefaultReference.INDEX_NAME_XAU_GBP),
	RULE_14 (DefaultReference.PORTFOLIO_UK_GOLD	, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD, DefaultReference.INDEX_NAME_XAU_USD),
	RULE_15 (DefaultReference.PORTFOLIO_UK_IRIDIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XIREUR,	TestIndex.INDEX_XIR_EUR, DefaultReference.INDEX_NAME_XIR_EUR),
	RULE_16 (DefaultReference.PORTFOLIO_UK_IRIDIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XIRGBP,	TestIndex.INDEX_XIR_GBP, DefaultReference.INDEX_NAME_XIR_GBP),
	RULE_17 (DefaultReference.PORTFOLIO_UK_IRIDIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD, DefaultReference.INDEX_NAME_XIR_USD),
	RULE_18 (DefaultReference.PORTFOLIO_UK_OSMIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XOSEUR,		TestIndex.INDEX_XOS_EUR, DefaultReference.INDEX_NAME_XOS_EUR),
	RULE_19 (DefaultReference.PORTFOLIO_UK_OSMIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XOSGBP,	TestIndex.INDEX_XOS_GBP, DefaultReference.INDEX_NAME_XOS_GBP),
	RULE_20 (DefaultReference.PORTFOLIO_UK_OSMIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD, DefaultReference.INDEX_NAME_XOS_USD),
	RULE_21 (DefaultReference.PORTFOLIO_UK_PALLADIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XPDEUR,	TestIndex.INDEX_XPD_EUR, DefaultReference.INDEX_NAME_XPD_EUR),
	RULE_22 (DefaultReference.PORTFOLIO_UK_PALLADIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XPDGBP,	TestIndex.INDEX_XPD_GBP, DefaultReference.INDEX_NAME_XPD_GBP),
	RULE_23 (DefaultReference.PORTFOLIO_UK_PALLADIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD, DefaultReference.INDEX_NAME_XPD_USD),
	RULE_24 (DefaultReference.PORTFOLIO_UK_PLATINUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XPTEUR,	TestIndex.INDEX_XPT_EUR, DefaultReference.INDEX_NAME_XPT_EUR),
	RULE_25 (DefaultReference.PORTFOLIO_UK_PLATINUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XPTGBP,	TestIndex.INDEX_XPT_GBP, DefaultReference.INDEX_NAME_XPT_GBP),
	RULE_26 (DefaultReference.PORTFOLIO_UK_PLATINUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD, DefaultReference.INDEX_NAME_XPT_USD),
	RULE_27 (DefaultReference.PORTFOLIO_UK_RHODIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XRHEUR,	TestIndex.INDEX_XRH_EUR, DefaultReference.INDEX_NAME_XRH_EUR),
	RULE_28 (DefaultReference.PORTFOLIO_UK_RHODIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XRHGBP,	TestIndex.INDEX_XRH_GBP, DefaultReference.INDEX_NAME_XRH_GBP),
	RULE_29 (DefaultReference.PORTFOLIO_UK_RHODIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD, DefaultReference.INDEX_NAME_XRH_USD),
	RULE_30 (DefaultReference.PORTFOLIO_UK_RUTHENIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XRUEUR,	TestIndex.INDEX_XRU_EUR, DefaultReference.INDEX_NAME_XRU_EUR),
	RULE_31 (DefaultReference.PORTFOLIO_UK_RUTHENIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XRUGBP,	TestIndex.INDEX_XRU_GBP, DefaultReference.INDEX_NAME_XRU_GBP),
	RULE_32 (DefaultReference.PORTFOLIO_UK_RUTHENIUM, TestBunit.JM_PMM_UK,	DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD, DefaultReference.INDEX_NAME_XRU_USD),
	RULE_33 (DefaultReference.PORTFOLIO_HK_GOLD	, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD, DefaultReference.INDEX_NAME_XAU_USD),
	RULE_34 (DefaultReference.PORTFOLIO_HK_SILVER	, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD, DefaultReference.INDEX_NAME_XAG_USD),
	RULE_35 (DefaultReference.PORTFOLIO_HK_IRIDIUM	, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD, DefaultReference.INDEX_NAME_XIR_USD),
	RULE_36 (DefaultReference.PORTFOLIO_HK_OSMIUM, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD, DefaultReference.INDEX_NAME_XOS_USD),
	RULE_37 (DefaultReference.PORTFOLIO_HK_PALLADIUM, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD, DefaultReference.INDEX_NAME_XPD_USD),
	RULE_38 (DefaultReference.PORTFOLIO_HK_PLATINUM, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD, DefaultReference.INDEX_NAME_XPT_USD),
	RULE_39 (DefaultReference.PORTFOLIO_HK_RHODIUM, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD, DefaultReference.INDEX_NAME_XRH_USD),
	RULE_40 (DefaultReference.PORTFOLIO_HK_RUTHENIUM, TestBunit.JM_PMM_HK,	DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD, DefaultReference.INDEX_NAME_XRU_USD),
	RULE_41 (DefaultReference.PORTFOLIO_SA_SILVER, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD, DefaultReference.INDEX_NAME_XAG_USD),
	RULE_42 (DefaultReference.PORTFOLIO_SA_GOLD, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD, DefaultReference.INDEX_NAME_XAU_USD),
	RULE_43 (DefaultReference.PORTFOLIO_SA_IRIDIUM, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD, DefaultReference.INDEX_NAME_XIR_USD),
	RULE_44 (DefaultReference.PORTFOLIO_SA_OSMIUM, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XOSUSD,	TestIndex.INDEX_XOS_USD, DefaultReference.INDEX_NAME_XOS_USD),
	RULE_45 (DefaultReference.PORTFOLIO_SA_PALLADIUM, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD, DefaultReference.INDEX_NAME_XPD_USD),
	RULE_46 (DefaultReference.PORTFOLIO_SA_PLATINUM, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD, DefaultReference.INDEX_NAME_XPT_USD),
	RULE_47 (DefaultReference.PORTFOLIO_SA_RHODIUM, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD, DefaultReference.INDEX_NAME_XRH_USD),
	RULE_48 (DefaultReference.PORTFOLIO_SA_RUTHENIUM, TestBunit.JM_PM_LTD,	DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD, DefaultReference.INDEX_NAME_XRU_USD),
	RULE_49 (DefaultReference.PORTFOLIO_CN_SILVER, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XAGCNY	,	TestIndex.INDEX_XAG_CNY, DefaultReference.INDEX_NAME_XAG_CNY),
	RULE_50 (DefaultReference.PORTFOLIO_CN_SILVER	, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XAGUSD,	TestIndex.INDEX_XAG_USD, DefaultReference.INDEX_NAME_XAG_USD),
	RULE_51 (DefaultReference.PORTFOLIO_CN_GOLD	, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XAUCNY,	TestIndex.INDEX_XAU_CNY, DefaultReference.INDEX_NAME_XAU_CNY),
	RULE_52 (DefaultReference.PORTFOLIO_CN_GOLD, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XAUUSD,	TestIndex.INDEX_XAU_USD, DefaultReference.INDEX_NAME_XAU_USD),
	RULE_53 (DefaultReference.PORTFOLIO_CN_IRIDIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XIRCNY,	TestIndex.INDEX_XIR_CNY, DefaultReference.INDEX_NAME_XIR_CNY),
	RULE_54 (DefaultReference.PORTFOLIO_CN_IRIDIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XIRUSD,	TestIndex.INDEX_XIR_USD, DefaultReference.INDEX_NAME_XIR_USD),
	RULE_55 (DefaultReference.PORTFOLIO_CN_PALLADIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XPDCNY,	TestIndex.INDEX_XPD_CNY, DefaultReference.INDEX_NAME_XPD_CNY),
	RULE_56 (DefaultReference.PORTFOLIO_CN_PALLADIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XPDUSD,	TestIndex.INDEX_XPD_USD, DefaultReference.INDEX_NAME_XPD_USD),
	RULE_57 (DefaultReference.PORTFOLIO_CN_PLATINUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XPTCNY,	TestIndex.INDEX_XPT_CNY, DefaultReference.INDEX_NAME_XPT_CNY),
	RULE_58 (DefaultReference.PORTFOLIO_CN_PLATINUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XPTUSD,	TestIndex.INDEX_XPT_USD, DefaultReference.INDEX_NAME_XPT_USD),
	RULE_60 (DefaultReference.PORTFOLIO_CN_RHODIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XRHCNY,	TestIndex.INDEX_XRH_CNY, DefaultReference.INDEX_NAME_XRH_CNY),
	RULE_61 (DefaultReference.PORTFOLIO_CN_RHODIUM	, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XRHUSD,	TestIndex.INDEX_XRH_USD, DefaultReference.INDEX_NAME_XRH_USD),
	RULE_62 (DefaultReference.PORTFOLIO_CN_RUTHENIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XRUCNY,	TestIndex.INDEX_XRU_CNY, DefaultReference.INDEX_NAME_XRU_CNY),
	RULE_63 (DefaultReference.PORTFOLIO_CN_RUTHENIUM, TestBunit.JM_PMM_CN,	DefaultReference.TICKER_XRUUSD,	TestIndex.INDEX_XRU_USD, DefaultReference.INDEX_NAME_XAG_USD)
	;
	
	private TickerPortfolioRuleTo entity;
	
	private TestTickerPortfolioRule (DefaultReference portfolio, TestBunit party, DefaultReference ticker, TestIndex index, DefaultReference indexName) {
		entity = ImmutableTickerPortfolioRuleTo.builder()
				.displayStringIndex(indexName.getEntity().name())
				.displayStringParty(party.getEntity().name())
				.displayStringPortfolio(portfolio.getEntity().name())
				.displayStringTicker(ticker.getEntity().name())
				.idIndex(index.getEntity().id())
				.idParty(party.getEntity().id())
				.idPortfolio(portfolio.getEntity().id())
				.idTicker(ticker.getEntity().id())
				.build();
	}

	public TickerPortfolioRuleTo getEntity () {
		return entity;
	}
	
	public void setEntity (TickerPortfolioRuleTo entity) {
		this.entity = entity;
	}
	
	
	public static List<TickerPortfolioRuleTo> asList () {
		return Arrays.asList(TestTickerPortfolioRule.values())
				.stream().map(TestTickerPortfolioRule::getEntity).collect(Collectors.toList());
	}

	public static List<TestTickerPortfolioRule> asEnumList () {
		return Arrays.asList(TestTickerPortfolioRule.values())
				.stream().collect(Collectors.toList());
	}
}

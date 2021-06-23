package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.transport.ImmutableIndexTo;
import com.matthey.pmm.toms.transport.IndexTo;

public enum TestIndex {
	INDEX_PX_XAG_CNY (DefaultReference.INDEX_NAME_PX_XAG_CNY, DefaultReference.METAL_XAG, DefaultReference.CCY_CNY, 1020668),
	INDEX_PX_XAG_EUR (DefaultReference.INDEX_NAME_PX_XAG_EUR, DefaultReference.METAL_XAG, DefaultReference.CCY_EUR, 1020182),
	INDEX_PX_XAG_GBP (DefaultReference.INDEX_NAME_PX_XAG_GBP, DefaultReference.METAL_XAG, DefaultReference.CCY_GBP, 1020183),
	INDEX_PX_XAG_USD (DefaultReference.INDEX_NAME_PX_XAG_USD, DefaultReference.METAL_XAG, DefaultReference.CCY_USD, 1020019),
	
	INDEX_PX_XAU_CNY (DefaultReference.INDEX_NAME_PX_XAU_CNY, DefaultReference.METAL_XAU, DefaultReference.CCY_CNY, 1020669),
	INDEX_PX_XAU_EUR (DefaultReference.INDEX_NAME_PX_XAU_EUR, DefaultReference.METAL_XAU, DefaultReference.CCY_EUR, 1020176),
	INDEX_PX_XAU_GBP (DefaultReference.INDEX_NAME_PX_XAU_GBP, DefaultReference.METAL_XAU, DefaultReference.CCY_GBP, 1020184),
	INDEX_PX_XAU_USD (DefaultReference.INDEX_NAME_PX_XAU_USD, DefaultReference.METAL_XAU, DefaultReference.CCY_USD, 1020014),
	
	INDEX_PX_XIR_CNY (DefaultReference.INDEX_NAME_PX_XIR_CNY, DefaultReference.METAL_XIR, DefaultReference.CCY_CNY, 1020670),
	INDEX_PX_XIR_EUR (DefaultReference.INDEX_NAME_PX_XIR_EUR, DefaultReference.METAL_XIR, DefaultReference.CCY_EUR, 1020175),
	INDEX_PX_XIR_GBP (DefaultReference.INDEX_NAME_PX_XIR_GBP, DefaultReference.METAL_XIR, DefaultReference.CCY_GBP, 1020185),
	INDEX_PX_XIR_USD (DefaultReference.INDEX_NAME_PX_XIR_USD, DefaultReference.METAL_XIR, DefaultReference.CCY_USD, 1020035),
	
	INDEX_PX_XOS_CNY (DefaultReference.INDEX_NAME_PX_XOS_CNY, DefaultReference.METAL_XOS, DefaultReference.CCY_CNY, 1020671),
	INDEX_PX_XOS_EUR (DefaultReference.INDEX_NAME_PX_XOS_EUR, DefaultReference.METAL_XOS, DefaultReference.CCY_EUR, 1020178),
	INDEX_PX_XOS_GBP (DefaultReference.INDEX_NAME_PX_XOS_GBP, DefaultReference.METAL_XOS, DefaultReference.CCY_GBP, 1020186),
	INDEX_PX_XOS_USD (DefaultReference.INDEX_NAME_PX_XOS_USD, DefaultReference.METAL_XOS, DefaultReference.CCY_USD, 1020038),
	
	INDEX_PX_XPD_CNY (DefaultReference.INDEX_NAME_PX_XPD_CNY, DefaultReference.METAL_XPD, DefaultReference.CCY_CNY, 1020672),
	INDEX_PX_XPD_EUR (DefaultReference.INDEX_NAME_PX_XPD_EUR, DefaultReference.METAL_XPD, DefaultReference.CCY_EUR, 1020174),
	INDEX_PX_XPD_GBP (DefaultReference.INDEX_NAME_PX_XPD_GBP, DefaultReference.METAL_XPD, DefaultReference.CCY_GBP, 1020187),
	INDEX_PX_XPD_USD (DefaultReference.INDEX_NAME_PX_XPD_USD, DefaultReference.METAL_XPD, DefaultReference.CCY_USD, 1020007),
	
	INDEX_PX_XPT_CNY (DefaultReference.INDEX_NAME_PX_XPT_CNY, DefaultReference.METAL_XPT, DefaultReference.CCY_CNY, 1020673),
	INDEX_PX_XPT_EUR (DefaultReference.INDEX_NAME_PX_XPT_EUR, DefaultReference.METAL_XPT, DefaultReference.CCY_EUR, 1020171),
	INDEX_PX_XPT_GBP (DefaultReference.INDEX_NAME_PX_XPT_GBP, DefaultReference.METAL_XPT, DefaultReference.CCY_GBP, 1020188),
	INDEX_PX_XPT_USD (DefaultReference.INDEX_NAME_PX_XPT_USD, DefaultReference.METAL_XPT, DefaultReference.CCY_USD, 1020025),
	
	INDEX_PX_XRH_CNY (DefaultReference.INDEX_NAME_PX_XRH_CNY, DefaultReference.METAL_XRH, DefaultReference.CCY_CNY, 1020674),
	INDEX_PX_XRH_EUR (DefaultReference.INDEX_NAME_PX_XRH_EUR, DefaultReference.METAL_XRH, DefaultReference.CCY_EUR, 1020179),
	INDEX_PX_XRH_GBP (DefaultReference.INDEX_NAME_PX_XRH_GBP, DefaultReference.METAL_XRH, DefaultReference.CCY_GBP, 1020189),
	INDEX_PX_XRH_USD (DefaultReference.INDEX_NAME_PX_XRH_USD, DefaultReference.METAL_XRH, DefaultReference.CCY_USD, 1020076),
	
	INDEX_PX_XRU_CNY (DefaultReference.INDEX_NAME_PX_XRU_CNY, DefaultReference.METAL_XRU, DefaultReference.CCY_CNY, 1020675),
	INDEX_PX_XRU_EUR (DefaultReference.INDEX_NAME_PX_XRU_EUR, DefaultReference.METAL_XRU, DefaultReference.CCY_EUR, 1020180),
	INDEX_PX_XRU_GBP (DefaultReference.INDEX_NAME_PX_XRU_GBP, DefaultReference.METAL_XRU, DefaultReference.CCY_GBP, 1020190),
	INDEX_PX_XRU_USD (DefaultReference.INDEX_NAME_PX_XRU_USD, DefaultReference.METAL_XRU, DefaultReference.CCY_USD, 1020010),
	
	INDEX_FX_EUR_CHF (DefaultReference.INDEX_NAME_FX_EUR_CHF, DefaultReference.CCY_EUR, DefaultReference.CCY_CHF, 1020082),
	INDEX_FX_EUR_CNY (DefaultReference.INDEX_NAME_FX_EUR_CNY, DefaultReference.CCY_EUR, DefaultReference.CCY_CNY, 1020066),
	INDEX_FX_EUR_GBP (DefaultReference.INDEX_NAME_FX_EUR_GBP, DefaultReference.CCY_EUR, DefaultReference.CCY_GBP, 1020053),
	INDEX_FX_EUR_HKD (DefaultReference.INDEX_NAME_FX_EUR_HKD, DefaultReference.CCY_EUR, DefaultReference.CCY_HKD, 1020074),
	INDEX_FX_EUR_USD (DefaultReference.INDEX_NAME_FX_EUR_USD, DefaultReference.CCY_EUR, DefaultReference.CCY_USD, 1020051),
	INDEX_FX_EUR_ZAR (DefaultReference.INDEX_NAME_FX_EUR_ZAR, DefaultReference.CCY_EUR, DefaultReference.CCY_ZAR, 1020073),

	INDEX_FX_GBP_CHF (DefaultReference.INDEX_NAME_FX_GBP_CHF, DefaultReference.CCY_GBP, DefaultReference.CCY_CHF, 1020128),
	INDEX_FX_GBP_CNY (DefaultReference.INDEX_NAME_FX_GBP_CNY, DefaultReference.CCY_GBP, DefaultReference.CCY_CNY, 1020075),
	INDEX_FX_GBP_HKD (DefaultReference.INDEX_NAME_FX_GBP_HKD, DefaultReference.CCY_GBP, DefaultReference.CCY_HKD, 1020072),
	INDEX_FX_GBP_USD (DefaultReference.INDEX_NAME_FX_GBP_USD, DefaultReference.CCY_GBP, DefaultReference.CCY_USD, 1020116),
	INDEX_FX_GBP_ZAR (DefaultReference.INDEX_NAME_FX_GBP_ZAR, DefaultReference.CCY_GBP, DefaultReference.CCY_ZAR, 1020045),
	
	INDEX_FX_HKD_USD (DefaultReference.INDEX_NAME_FX_HKD_USD, DefaultReference.CCY_HKD, DefaultReference.CCY_USD, 1020144),

	INDEX_FX_USD_CHF (DefaultReference.INDEX_NAME_FX_USD_CHF, DefaultReference.CCY_USD, DefaultReference.CCY_CHF, 1020083),
	INDEX_FX_USD_CNY (DefaultReference.INDEX_NAME_FX_USD_CNY, DefaultReference.CCY_USD, DefaultReference.CCY_CNY, 1020064),
	INDEX_FX_USD_HKD (DefaultReference.INDEX_NAME_FX_USD_HKD, DefaultReference.CCY_USD, DefaultReference.CCY_HKD, 1020071),
	INDEX_FX_USD_ZAR (DefaultReference.INDEX_NAME_FX_USD_ZAR, DefaultReference.CCY_USD, DefaultReference.CCY_ZAR, 1020070),	
	;
	
	private final IndexTo index;
	
	private TestIndex (DefaultReference indexName, DefaultReference currencyOne, DefaultReference currencyTwo, int endurId) {
		this.index = ImmutableIndexTo.builder()
				.id(endurId)
				.idIndexName(indexName.getEntity().id())
				.idCurrencyOneName(currencyOne.getEntity().id())
				.idCurrencyTwoName(currencyTwo.getEntity().id())
				.build();
	}

	public IndexTo getEntity () {
		return index;
	}

	public static List<IndexTo> asList () {
		return Arrays.asList(TestIndex.values())
				.stream().map(TestIndex::getEntity).collect(Collectors.toList());
	}
	
	public static List<IndexTo> asListMetal () {
		return Arrays.asList(TestIndex.values()).stream()
				.filter(x -> DefaultReference.findById (x.getEntity().idCurrencyOneName()).get().idType() == DefaultReferenceType.CCY_METAL.getEntity().id()
				           || DefaultReference.findById (x.getEntity().idCurrencyTwoName()).get().idType() == DefaultReferenceType.CCY_METAL.getEntity().id())
				.map(TestIndex::getEntity)
				.collect(Collectors.toList());		
	}

	public static List<IndexTo> asListCurrency () {
		return Arrays.asList(TestIndex.values()).stream()
				.filter(x -> DefaultReference.findById (x.getEntity().idCurrencyOneName()).get().idType() != DefaultReferenceType.CCY_METAL.getEntity().id()
				           && DefaultReference.findById (x.getEntity().idCurrencyTwoName()).get().idType() != DefaultReferenceType.CCY_METAL.getEntity().id())
				.map(TestIndex::getEntity)
				.collect(Collectors.toList());		
	}

	
}
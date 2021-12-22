package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.transport.ImmutableIndexTo;
import com.matthey.pmm.toms.transport.IndexTo;

public enum TestIndex {
	INDEX_PX_XAG_CNY (DefaultReference.INDEX_NAME_PX_XAG_CNY, DefaultReference.METAL_XAG, DefaultReference.CCY_CNY, 1020668, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 100l),
	INDEX_PX_XAG_EUR (DefaultReference.INDEX_NAME_PX_XAG_EUR, DefaultReference.METAL_XAG, DefaultReference.CCY_EUR, 1020182, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 200l),
	INDEX_PX_XAG_GBP (DefaultReference.INDEX_NAME_PX_XAG_GBP, DefaultReference.METAL_XAG, DefaultReference.CCY_GBP, 1020183, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 300l),
	INDEX_PX_XAG_USD (DefaultReference.INDEX_NAME_PX_XAG_USD, DefaultReference.METAL_XAG, DefaultReference.CCY_USD, 1020019, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 400l),
	
	INDEX_PX_XAU_CNY (DefaultReference.INDEX_NAME_PX_XAU_CNY, DefaultReference.METAL_XAU, DefaultReference.CCY_CNY, 1020669, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 500l),
	INDEX_PX_XAU_EUR (DefaultReference.INDEX_NAME_PX_XAU_EUR, DefaultReference.METAL_XAU, DefaultReference.CCY_EUR, 1020176, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 600l),
	INDEX_PX_XAU_GBP (DefaultReference.INDEX_NAME_PX_XAU_GBP, DefaultReference.METAL_XAU, DefaultReference.CCY_GBP, 1020184, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 700l),
	INDEX_PX_XAU_USD (DefaultReference.INDEX_NAME_PX_XAU_USD, DefaultReference.METAL_XAU, DefaultReference.CCY_USD, 1020014, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 800l),
	
	INDEX_PX_XIR_CNY (DefaultReference.INDEX_NAME_PX_XIR_CNY, DefaultReference.METAL_XIR, DefaultReference.CCY_CNY, 1020670, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 900l),
	INDEX_PX_XIR_EUR (DefaultReference.INDEX_NAME_PX_XIR_EUR, DefaultReference.METAL_XIR, DefaultReference.CCY_EUR, 1020175, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1000l),
	INDEX_PX_XIR_GBP (DefaultReference.INDEX_NAME_PX_XIR_GBP, DefaultReference.METAL_XIR, DefaultReference.CCY_GBP, 1020185, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1100l),
	INDEX_PX_XIR_USD (DefaultReference.INDEX_NAME_PX_XIR_USD, DefaultReference.METAL_XIR, DefaultReference.CCY_USD, 1020035, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1200l),
	
	INDEX_PX_XOS_CNY (DefaultReference.INDEX_NAME_PX_XOS_CNY, DefaultReference.METAL_XOS, DefaultReference.CCY_CNY, 1020671, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1300l),
	INDEX_PX_XOS_EUR (DefaultReference.INDEX_NAME_PX_XOS_EUR, DefaultReference.METAL_XOS, DefaultReference.CCY_EUR, 1020178, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1400l),
	INDEX_PX_XOS_GBP (DefaultReference.INDEX_NAME_PX_XOS_GBP, DefaultReference.METAL_XOS, DefaultReference.CCY_GBP, 1020186, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1500l),
	INDEX_PX_XOS_USD (DefaultReference.INDEX_NAME_PX_XOS_USD, DefaultReference.METAL_XOS, DefaultReference.CCY_USD, 1020038, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1600l),
	
	INDEX_PX_XPD_CNY (DefaultReference.INDEX_NAME_PX_XPD_CNY, DefaultReference.METAL_XPD, DefaultReference.CCY_CNY, 1020672, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1700l),
	INDEX_PX_XPD_EUR (DefaultReference.INDEX_NAME_PX_XPD_EUR, DefaultReference.METAL_XPD, DefaultReference.CCY_EUR, 1020174, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1800l),	
	INDEX_PX_XPD_GBP (DefaultReference.INDEX_NAME_PX_XPD_GBP, DefaultReference.METAL_XPD, DefaultReference.CCY_GBP, 1020187, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 1900l),
	INDEX_PX_XPD_USD (DefaultReference.INDEX_NAME_PX_XPD_USD, DefaultReference.METAL_XPD, DefaultReference.CCY_USD, 1020007, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2000l),
	
	INDEX_PX_XPT_CNY (DefaultReference.INDEX_NAME_PX_XPT_CNY, DefaultReference.METAL_XPT, DefaultReference.CCY_CNY, 1020673, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2100l),
	INDEX_PX_XPT_EUR (DefaultReference.INDEX_NAME_PX_XPT_EUR, DefaultReference.METAL_XPT, DefaultReference.CCY_EUR, 1020171, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2200l),
	INDEX_PX_XPT_GBP (DefaultReference.INDEX_NAME_PX_XPT_GBP, DefaultReference.METAL_XPT, DefaultReference.CCY_GBP, 1020188, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2300l),
	INDEX_PX_XPT_USD (DefaultReference.INDEX_NAME_PX_XPT_USD, DefaultReference.METAL_XPT, DefaultReference.CCY_USD, 1020025, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2400l),
	
	INDEX_PX_XRH_CNY (DefaultReference.INDEX_NAME_PX_XRH_CNY, DefaultReference.METAL_XRH, DefaultReference.CCY_CNY, 1020674, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2500l),
	INDEX_PX_XRH_EUR (DefaultReference.INDEX_NAME_PX_XRH_EUR, DefaultReference.METAL_XRH, DefaultReference.CCY_EUR, 1020179, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2600l),
	INDEX_PX_XRH_GBP (DefaultReference.INDEX_NAME_PX_XRH_GBP, DefaultReference.METAL_XRH, DefaultReference.CCY_GBP, 1020189, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2700l),
	INDEX_PX_XRH_USD (DefaultReference.INDEX_NAME_PX_XRH_USD, DefaultReference.METAL_XRH, DefaultReference.CCY_USD, 1020076, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2800l),
	
	INDEX_PX_XRU_CNY (DefaultReference.INDEX_NAME_PX_XRU_CNY, DefaultReference.METAL_XRU, DefaultReference.CCY_CNY, 1020675, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 2900l),
	INDEX_PX_XRU_EUR (DefaultReference.INDEX_NAME_PX_XRU_EUR, DefaultReference.METAL_XRU, DefaultReference.CCY_EUR, 1020180, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3000l),
	INDEX_PX_XRU_GBP (DefaultReference.INDEX_NAME_PX_XRU_GBP, DefaultReference.METAL_XRU, DefaultReference.CCY_GBP, 1020190, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3100l),
	INDEX_PX_XRU_USD (DefaultReference.INDEX_NAME_PX_XRU_USD, DefaultReference.METAL_XRU, DefaultReference.CCY_USD, 1020010, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3200l),
	
	INDEX_FX_EUR_CNY (DefaultReference.INDEX_NAME_FX_EUR_CNY, DefaultReference.CCY_EUR, DefaultReference.CCY_CNY, 1020066, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3400l),
	INDEX_FX_EUR_GBP (DefaultReference.INDEX_NAME_FX_EUR_GBP, DefaultReference.CCY_EUR, DefaultReference.CCY_GBP, 1020053, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3500l),
	INDEX_FX_EUR_USD (DefaultReference.INDEX_NAME_FX_EUR_USD, DefaultReference.CCY_EUR, DefaultReference.CCY_USD, 1020051, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3700l),
	INDEX_FX_EUR_ZAR (DefaultReference.INDEX_NAME_FX_EUR_ZAR, DefaultReference.CCY_EUR, DefaultReference.CCY_ZAR, 1020073, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 3800l),

	INDEX_FX_GBP_CNY (DefaultReference.INDEX_NAME_FX_GBP_CNY, DefaultReference.CCY_GBP, DefaultReference.CCY_CNY, 1020075, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 4000l),
	INDEX_FX_GBP_USD (DefaultReference.INDEX_NAME_FX_GBP_USD, DefaultReference.CCY_GBP, DefaultReference.CCY_USD, 1020116, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 4200l),
	INDEX_FX_GBP_ZAR (DefaultReference.INDEX_NAME_FX_GBP_ZAR, DefaultReference.CCY_GBP, DefaultReference.CCY_ZAR, 1020045, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 4300l),
	
	INDEX_FX_USD_CNY (DefaultReference.INDEX_NAME_FX_USD_CNY, DefaultReference.CCY_USD, DefaultReference.CCY_CNY, 1020064, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 4600l),
	INDEX_FX_USD_ZAR (DefaultReference.INDEX_NAME_FX_USD_ZAR, DefaultReference.CCY_USD, DefaultReference.CCY_ZAR, 1020070, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 4800l),	
	
	INDEX_XAU_USD (DefaultReference.INDEX_NAME_XAU_USD,	DefaultReference.METAL_XAU,	DefaultReference.CCY_USD,	1020049, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 4900l),
	INDEX_XAG_USD (DefaultReference.INDEX_NAME_XAG_USD,	DefaultReference.METAL_XAG,	DefaultReference.CCY_USD,	1020056, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5000l),
	INDEX_XOS_USD (DefaultReference.INDEX_NAME_XOS_USD,	DefaultReference.METAL_XOS,	DefaultReference.CCY_USD,	1020058, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5100l),
	INDEX_XRU_USD (DefaultReference.INDEX_NAME_XRU_USD,	DefaultReference.METAL_XRU,	DefaultReference.CCY_USD,	1020060, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5200l),
	INDEX_XPD_USD (DefaultReference.INDEX_NAME_XPD_USD,	DefaultReference.METAL_XPD,	DefaultReference.CCY_USD,	1020061, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5300l),
	INDEX_XPT_USD (DefaultReference.INDEX_NAME_XPT_USD,	DefaultReference.METAL_XPT,	DefaultReference.CCY_USD,	1020065, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5400l),
	INDEX_XIR_USD (DefaultReference.INDEX_NAME_XIR_USD,	DefaultReference.METAL_XIR,	DefaultReference.CCY_USD,	1020069, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5500l),
	INDEX_XRH_USD (DefaultReference.INDEX_NAME_XRH_USD,	DefaultReference.METAL_XRH,	DefaultReference.CCY_USD,	1020077, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5600l),
	INDEX_XPT_EUR (DefaultReference.INDEX_NAME_XPT_EUR,	DefaultReference.METAL_XPT,	DefaultReference.CCY_EUR,	1020172, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XIR_EUR (DefaultReference.INDEX_NAME_XIR_EUR,	DefaultReference.METAL_XIR,	DefaultReference.CCY_EUR,	1020193, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XOS_EUR (DefaultReference.INDEX_NAME_XOS_EUR,	DefaultReference.METAL_XOS,	DefaultReference.CCY_EUR,	1020194, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XRH_EUR (DefaultReference.INDEX_NAME_XRH_EUR,	DefaultReference.METAL_XRH,	DefaultReference.CCY_EUR,	1020196, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XRU_EUR (DefaultReference.INDEX_NAME_XRU_EUR,	DefaultReference.METAL_XRU,	DefaultReference.CCY_EUR,	1020197, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XAG_EUR (DefaultReference.INDEX_NAME_XAG_EUR,	DefaultReference.METAL_XAG,	DefaultReference.CCY_EUR,	1020191, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5800l),
	INDEX_XAU_EUR (DefaultReference.INDEX_NAME_XAU_EUR,	DefaultReference.METAL_XAU,	DefaultReference.CCY_EUR,	1020192, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5900l),
	INDEX_XPD_EUR (DefaultReference.INDEX_NAME_XPD_EUR,	DefaultReference.METAL_XPD,	DefaultReference.CCY_EUR,	1020195, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6000l),
	INDEX_XIR_GBP (DefaultReference.INDEX_NAME_XIR_GBP,	DefaultReference.METAL_XIR,	DefaultReference.CCY_GBP,	1020200, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XOS_GBP (DefaultReference.INDEX_NAME_XOS_GBP,	DefaultReference.METAL_XOS,	DefaultReference.CCY_GBP,	1020201, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XRH_GBP (DefaultReference.INDEX_NAME_XRH_GBP,	DefaultReference.METAL_XRH,	DefaultReference.CCY_GBP,	1020204, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XRU_GBP (DefaultReference.INDEX_NAME_XRU_GBP,	DefaultReference.METAL_XRU,	DefaultReference.CCY_GBP,	1020205, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 5700l),
	INDEX_XAG_GBP (DefaultReference.INDEX_NAME_XAG_GBP,	DefaultReference.METAL_XAG,	DefaultReference.CCY_GBP,	1020198, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6100l),
	INDEX_XAU_GBP (DefaultReference.INDEX_NAME_XAU_GBP,	DefaultReference.METAL_XAU,	DefaultReference.CCY_GBP,	1020199, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6200l),
	INDEX_XPD_GBP (DefaultReference.INDEX_NAME_XPD_GBP,	DefaultReference.METAL_XPD,	DefaultReference.CCY_GBP,	1020202, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6300l),
	INDEX_XPT_GBP (DefaultReference.INDEX_NAME_XPT_GBP,	DefaultReference.METAL_XPT,	DefaultReference.CCY_GBP,	1020203, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6400l),
	INDEX_XRH_CNY (DefaultReference.INDEX_NAME_XRH_CNY,	DefaultReference.METAL_XRH,	DefaultReference.CCY_CNY,	1020543, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6500l),
	INDEX_XAG_CNY (DefaultReference.INDEX_NAME_XAG_CNY,	DefaultReference.METAL_XAG,	DefaultReference.CCY_CNY,	1020676, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6600l),
	INDEX_XAU_CNY (DefaultReference.INDEX_NAME_XAU_CNY,	DefaultReference.METAL_XAU,	DefaultReference.CCY_CNY,	1020677, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6700l),
	INDEX_XIR_CNY (DefaultReference.INDEX_NAME_XIR_CNY,	DefaultReference.METAL_XIR,	DefaultReference.CCY_CNY,	1020678, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6800l),
	INDEX_XOS_CNY (DefaultReference.INDEX_NAME_XOS_CNY,	DefaultReference.METAL_XOS,	DefaultReference.CCY_CNY,	1020679, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 6900l),
	INDEX_XPD_CNY (DefaultReference.INDEX_NAME_XPD_CNY,	DefaultReference.METAL_XPD,	DefaultReference.CCY_CNY,	1020680, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 7000l),
	INDEX_XPT_CNY (DefaultReference.INDEX_NAME_XPT_CNY,	DefaultReference.METAL_XPT,	DefaultReference.CCY_CNY,	1020681, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 7100l),
	INDEX_XRU_CNY (DefaultReference.INDEX_NAME_XRU_CNY,	DefaultReference.METAL_XRU,	DefaultReference.CCY_CNY,	1020683, DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, 7200l),

	;
	
	private final IndexTo index;
	
	private TestIndex (DefaultReference indexName, DefaultReference currencyOne, DefaultReference currencyTwo, long endurId, 
			DefaultReference lifecycleStatus, Long sortColumn) {
		this.index = ImmutableIndexTo.builder()
				.id(endurId)
				.idIndexName(indexName.getEntity().id())
				.idCurrencyOneName(currencyOne.getEntity().id())
				.idCurrencyTwoName(currencyTwo.getEntity().id())
				.idLifecycle(lifecycleStatus.getEntity().id())
				.sortColumn(sortColumn)
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
package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableReference;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultReference {
	PARTY_TYPE_INTERNAL_BUNIT(DefaultReferenceType.PARTY_TYPE, 1, "Internal Business Unit"),
	PARTY_TYPE_EXTERNAL_BUNIT(DefaultReferenceType.PARTY_TYPE, 2, "External Business Unit"),
	USER_ROLE_PMM_USER (DefaultReferenceType.USER_ROLE, 3, "PMM User"),
	USER_ROLE_PMM_TRADER (DefaultReferenceType.USER_ROLE, 4, "PMM Trader"),
	USER_ROLE_ADMIN (DefaultReferenceType.USER_ROLE, 5, "Admin"),
	USER_ROLE_SERVICE_USER (DefaultReferenceType.USER_ROLE, 6, "Service User"),
	ORDER_STATUS_NEW (DefaultReferenceType.ORDER_STATUS_NAME, 7, "New"),
	ORDER_STATUS_PARTIAL (DefaultReferenceType.ORDER_STATUS_NAME, 8, "Partially Filled"),
	ORDER_STATUS_FILLED (DefaultReferenceType.ORDER_STATUS_NAME, 9, "Filled"),
	ORDER_STATUS_CANCELLED (DefaultReferenceType.ORDER_STATUS_NAME, 10, "Cancelled"),
	ORDER_STATUS_WAITING_APPROVAL (DefaultReferenceType.ORDER_STATUS_NAME, 11, "Waiting Approval"),
	ORDER_STATUS_APPROVED (DefaultReferenceType.ORDER_STATUS_NAME, 12, "Approved"),
	ORDER_TYPE_LIMIT_ORDER (DefaultReferenceType.ORDER_TYPE_NAME, 13, "Limit Order"),
	ORDER_TYPE_REFERENCE_ORDER (DefaultReferenceType.ORDER_TYPE_NAME, 14, "Reference Order"),
	BUY_SELL_BUY (DefaultReferenceType.BUY_SELL, 15, "Buy", 0),
	BUY_SELL_SELL (DefaultReferenceType.BUY_SELL, 16, "Sell", 1),
	EXPIRATION_STATUS_ACTIVE (DefaultReferenceType.EXPIRATION_STATUS, 17, "Active"),
	EXPIRATION_STATUS_EXPIRED (DefaultReferenceType.EXPIRATION_STATUS, 18, "Expired"),
	PARTY_TYPE_INTERNAL_LE(DefaultReferenceType.PARTY_TYPE, 19, "Internal Legal Entity"),
	PARTY_TYPE_EXTERNAL_LE(DefaultReferenceType.PARTY_TYPE, 20, "External Legal Entity"),
	CACHE_TYPE_PARTY_TYPE (DefaultReferenceType.CACHE_TYPE, 21, "Party Cache"),
	CACHE_TYPE_USER_ROLE (DefaultReferenceType.CACHE_TYPE, 22, "User Role Cache"),
	CACHE_TYPE_ORDER_STATUS (DefaultReferenceType.CACHE_TYPE, 23, "Order Status Cache"),
	CACHE_TYPE_ORDER_TYPE (DefaultReferenceType.CACHE_TYPE, 24, "Order Type Cache"),
	CACHE_TYPE_BUY_SELL (DefaultReferenceType.CACHE_TYPE, 25, "Buy/Sell Cache"),
	CACHE_EXPIRATION_STATUS (DefaultReferenceType.CACHE_TYPE, 25, "Expiration Status Cache"),
	CACHE_USER (DefaultReferenceType.CACHE_TYPE, 26, "User Cache"),
	CACHE_PARTY (DefaultReferenceType.CACHE_TYPE, 27, "Party Cache"),
	QUANTITY_TOZ (DefaultReferenceType.QUANTITY_UNIT, 28, "TOz", 55),
	QUANTITY_MT (DefaultReferenceType.QUANTITY_UNIT, 29, "MT", 13),
	QUANTITY_GMS (DefaultReferenceType.QUANTITY_UNIT, 30, "gms", 51),
	QUANTITY_KGS (DefaultReferenceType.QUANTITY_UNIT, 31, "kgs", 52),
	QUANTITY_LBS (DefaultReferenceType.QUANTITY_UNIT, 32, "lbs", 53),
	QUANTITY_MGS (DefaultReferenceType.QUANTITY_UNIT, 33, "mgs", 54),
	METAL_XRU (DefaultReferenceType.CCY_METAL, 34, "XRU", 63),
	METAL_XOS (DefaultReferenceType.CCY_METAL, 35, "XOS", 62),
	METAL_XIR (DefaultReferenceType.CCY_METAL, 36, "XIR", 61),
	METAL_XAG (DefaultReferenceType.CCY_METAL, 37, "XAG", 53),
	METAL_XPT (DefaultReferenceType.CCY_METAL, 38, "XPT", 56),
	METAL_XRH (DefaultReferenceType.CCY_METAL, 39, "XRH", 58),
	METAL_XAU (DefaultReferenceType.CCY_METAL, 40, "XAU", 54),
	METAL_XPD (DefaultReferenceType.CCY_METAL, 41, "XPD", 55),
	CCY_USD (DefaultReferenceType.CCY_CURRENCY, 42, "USD", 0),
	CCY_EUR (DefaultReferenceType.CCY_CURRENCY, 43, "EUR", 51),
	CCY_CNY (DefaultReferenceType.CCY_CURRENCY, 44, "CNY", 60),
	CCY_HKD (DefaultReferenceType.CCY_CURRENCY, 45, "HKD", 59),
	CCY_ZAR (DefaultReferenceType.CCY_CURRENCY, 46, "ZAR", 57),
	CCY_CHF (DefaultReferenceType.CCY_CURRENCY, 47, "CHF", 64),
	CCY_GBP (DefaultReferenceType.CCY_CURRENCY, 48, "GBP", 52),
	INDEX_NAME_PX_XAG_CNY (DefaultReferenceType.INDEX_NAME, 49, "PX_XAG.CNY"),
	INDEX_NAME_PX_XAG_EUR (DefaultReferenceType.INDEX_NAME, 50, "PX_XAG.EUR"),
	INDEX_NAME_PX_XAG_GBP (DefaultReferenceType.INDEX_NAME, 51, "PX_XAG.GBP"),
	INDEX_NAME_PX_XAG_USD (DefaultReferenceType.INDEX_NAME, 52, "PX_XAG.USD"),
	INDEX_NAME_PX_XAU_CNY (DefaultReferenceType.INDEX_NAME, 53, "PX_XAU.CNY"),
	INDEX_NAME_PX_XAU_EUR (DefaultReferenceType.INDEX_NAME, 54, "PX_XAU.EUR"),
	INDEX_NAME_PX_XAU_GBP (DefaultReferenceType.INDEX_NAME, 55, "PX_XAU.GBP"),
	INDEX_NAME_PX_XAU_USD (DefaultReferenceType.INDEX_NAME, 56, "PX_XAU.USD"),
	INDEX_NAME_PX_XIR_CNY (DefaultReferenceType.INDEX_NAME, 57, "PX_XIR.CNY"),
	INDEX_NAME_PX_XIR_EUR (DefaultReferenceType.INDEX_NAME, 58, "PX_XIR.EUR"),
	INDEX_NAME_PX_XIR_GBP (DefaultReferenceType.INDEX_NAME, 59, "PX_XIR.GBP"),
	INDEX_NAME_PX_XIR_USD (DefaultReferenceType.INDEX_NAME, 60, "PX_XIR.USD"),
	INDEX_NAME_PX_XOS_CNY (DefaultReferenceType.INDEX_NAME, 61, "PX_XOS.CNY"),
	INDEX_NAME_PX_XOS_EUR (DefaultReferenceType.INDEX_NAME, 62, "PX_XOS.EUR"),
	INDEX_NAME_PX_XOS_GBP (DefaultReferenceType.INDEX_NAME, 63, "PX_XOS.GBP"),
	INDEX_NAME_PX_XOS_USD (DefaultReferenceType.INDEX_NAME, 64, "PX_XOS.USD"),
	INDEX_NAME_PX_XPD_CNY (DefaultReferenceType.INDEX_NAME, 65, "PX_XPD.CNY"),
	INDEX_NAME_PX_XPD_EUR (DefaultReferenceType.INDEX_NAME, 66, "PX_XPD.EUR"),
	INDEX_NAME_PX_XPD_GBP (DefaultReferenceType.INDEX_NAME, 67, "PX_XPD.GBP"),
	INDEX_NAME_PX_XPD_USD (DefaultReferenceType.INDEX_NAME, 68, "PX_XPD.USD"),
	INDEX_NAME_PX_XPT_CNY (DefaultReferenceType.INDEX_NAME, 69, "PX_XPT.CNY"),
	INDEX_NAME_PX_XPT_EUR (DefaultReferenceType.INDEX_NAME, 70, "PX_XPT.EUR"),
	INDEX_NAME_PX_XPT_GBP (DefaultReferenceType.INDEX_NAME, 71, "PX_XPT.GBP"),
	INDEX_NAME_PX_XPT_USD (DefaultReferenceType.INDEX_NAME, 72, "PX_XPT.USD"),
	INDEX_NAME_PX_XRH_CNY (DefaultReferenceType.INDEX_NAME, 73, "PX_XRH.CNY"),
	INDEX_NAME_PX_XRH_EUR (DefaultReferenceType.INDEX_NAME, 74, "PX_XRH.EUR"),
	INDEX_NAME_PX_XRH_GBP (DefaultReferenceType.INDEX_NAME, 75, "PX_XRH.GBP"),
	INDEX_NAME_PX_XRH_USD (DefaultReferenceType.INDEX_NAME, 76, "PX_XRH.USD"),
	INDEX_NAME_PX_XRU_CNY (DefaultReferenceType.INDEX_NAME, 77, "PX_XRU.CNY"),
	INDEX_NAME_PX_XRU_EUR (DefaultReferenceType.INDEX_NAME, 78, "PX_XRU.EUR"),
	INDEX_NAME_PX_XRU_GBP (DefaultReferenceType.INDEX_NAME, 79, "PX_XRU.GBP"),
	INDEX_NAME_PX_XRU_USD (DefaultReferenceType.INDEX_NAME, 80, "PX_XRU.USD"),
	INDEX_NAME_FX_EUR_CHF (DefaultReferenceType.INDEX_NAME, 81, "FX_EUR.CHF"),
	INDEX_NAME_FX_EUR_CNY (DefaultReferenceType.INDEX_NAME, 82, "FX_EUR.CNY"),
	INDEX_NAME_FX_EUR_GBP (DefaultReferenceType.INDEX_NAME, 83, "FX_EUR.GBP"),
	INDEX_NAME_FX_EUR_HKD (DefaultReferenceType.INDEX_NAME, 84, "FX_EUR.HKD"),
	INDEX_NAME_FX_EUR_USD (DefaultReferenceType.INDEX_NAME, 85, "FX_EUR.USD"),
	INDEX_NAME_FX_EUR_ZAR (DefaultReferenceType.INDEX_NAME, 86, "FX_EUR.ZAR"),
	INDEX_NAME_FX_GBP_CHF (DefaultReferenceType.INDEX_NAME, 87, "FX_GBP.CHF"),
	INDEX_NAME_FX_GBP_CNY (DefaultReferenceType.INDEX_NAME, 88, "FX_GBP.CNY"),
	INDEX_NAME_FX_GBP_HKD (DefaultReferenceType.INDEX_NAME, 89, "FX_GBP.HKD"),
	INDEX_NAME_FX_GBP_USD (DefaultReferenceType.INDEX_NAME, 90, "FX_GBP.USD"),
	INDEX_NAME_FX_GBP_ZAR (DefaultReferenceType.INDEX_NAME, 91, "FX_GBP.ZAR"),
	INDEX_NAME_FX_HKD_USD (DefaultReferenceType.INDEX_NAME, 92, "FX_HKD.USD"),
	INDEX_NAME_FX_USD_CHF (DefaultReferenceType.INDEX_NAME, 93, "FX_USD.CHF"),
	INDEX_NAME_FX_USD_CNY (DefaultReferenceType.INDEX_NAME, 94, "FX_USD.CNY"),
	INDEX_NAME_FX_USD_HKD (DefaultReferenceType.INDEX_NAME, 95, "FX_USD.HKD"),
	INDEX_NAME_FX_USD_ZAR (DefaultReferenceType.INDEX_NAME, 96, "FX_USD.ZAR"),
	YES_NO_YES (DefaultReferenceType.YES_NO, 97, "Yes", 1),
	YES_NO_NO (DefaultReferenceType.YES_NO, 98, "No", 0),	
	PAYMENT_PERIOD_SAMPLE1 (DefaultReferenceType.PAYMENT_PERIOD, 99, "Sample 1 Payment Period"), // Endur Side ID?
	PAYMENT_PERIOD_SAMPLE2 (DefaultReferenceType.PAYMENT_PERIOD, 100, "Sample 2 Payment Period"),
	CREDIT_LIMIT_CHECK_STATUS_OPEN (DefaultReferenceType.CREDIT_LIMIT_CHECK_STATUS, 101, "Open"),
	CREDIT_LIMIT_CHECK_STATUS_IN_PROGRESS (DefaultReferenceType.CREDIT_LIMIT_CHECK_STATUS, 102, "In Progress"),
	CREDIT_LIMIT_CHECK_STATUS_COMPLETED (DefaultReferenceType.CREDIT_LIMIT_CHECK_STATUS, 103, "Completed"),
	CREDIT_LIMIT_CHECK_STATUS_FAILED (DefaultReferenceType.CREDIT_LIMIT_CHECK_STATUS, 104, "Failed"),
	PRICE_TYPE_SAMPLE1 (DefaultReferenceType.PRICE_TYPE, 105, "Sample 1 Price Type"),// Endur Side ID?
	PRICE_TYPE_SAMPLE2 (DefaultReferenceType.PRICE_TYPE, 106, "Sample 2 Price Type"),
	AVERAGING_RULES_SAMPLE1 (DefaultReferenceType.AVERAGING_RULE, 107, "Sample 1 Averaging Rule"),
	AVERAGING_RULES_SAMPLE2 (DefaultReferenceType.AVERAGING_RULE, 108, "Sample 2 Averaging Rule"),	
	STOP_TRIGGER_TYPE_SAMPLE1 (DefaultReferenceType.STOP_TRIGGER_TYPE, 109, "Sample 1 Stop Trigger Type"),	
	STOP_TRIGGER_TYPE_SAMPLE2 (DefaultReferenceType.STOP_TRIGGER_TYPE, 110, "Sample 2 Stop Trigger Type"),	
	;
	
	private final Reference ref;
	private final ReferenceType refType;
	
	private DefaultReference (DefaultReferenceType type, int id, String name) {
		this.refType = type.getEntity();
		ref = ImmutableReference.builder()
				.id(id)
				.name(name)
				.endurId(-1)
				.typeId(type.getEntity().id())
				.build();
	}
	
	private DefaultReference (DefaultReferenceType type, int id, String name, int endurId) {
		this.refType = type.getEntity();
		ref = ImmutableReference.builder()
				.id(id)
				.name(name)
				.endurId(endurId)
				.typeId(type.getEntity().id())
				.build();
	}

	public Reference getEntity() {
		return ref;
	}

	public ReferenceType getRefType() {
		return refType;
	}

	public static List<Reference> asList () {
		return Arrays.asList(DefaultReference.values())
				.stream().map(DefaultReference::getEntity).collect(Collectors.toList());
	}
	
	public static Optional<Reference> findById(int refId) {
		List<Reference> filtered = asList().stream().filter(x -> x.id() == refId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}

	public static Optional<List<Reference>> findByTypeId(int typeId) {
		List<Reference> filtered = asList().stream().filter(x -> x.typeId() == typeId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered);
		}
	}	
}

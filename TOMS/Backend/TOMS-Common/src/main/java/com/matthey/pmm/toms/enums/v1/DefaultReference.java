package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

public enum DefaultReference {
	PARTY_TYPE_INTERNAL_BUNIT(DefaultReferenceType.PARTY_TYPE, 1, "Internal Business Unit"),
	PARTY_TYPE_EXTERNAL_BUNIT(DefaultReferenceType.PARTY_TYPE, 2, "External Business Unit"),
	USER_ROLE_PMM_USER (DefaultReferenceType.USER_ROLE, 3, "PMM User"),
	USER_ROLE_PMM_TRADER (DefaultReferenceType.USER_ROLE, 4, "PMM Trader"),
	USER_ROLE_ADMIN (DefaultReferenceType.USER_ROLE, 5, "Admin"),
	USER_ROLE_SERVICE_USER (DefaultReferenceType.USER_ROLE, 6, "Service User"),
	ORDER_STATUS_PENDING (DefaultReferenceType.ORDER_STATUS_NAME, 7, "Pending"),
	ORDER_STATUS_FILLED (DefaultReferenceType.ORDER_STATUS_NAME, 9, "Filled"),
	ORDER_STATUS_CANCELLED (DefaultReferenceType.ORDER_STATUS_NAME, 10, "Cancelled"),
	ORDER_STATUS_WAITING_APPROVAL (DefaultReferenceType.ORDER_STATUS_NAME, 11, "Waiting Approval"),
	ORDER_STATUS_CONFIRMED (DefaultReferenceType.ORDER_STATUS_NAME, 12, "Confirmed"),
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
	CACHE_USER (DefaultReferenceType.CACHE_TYPE, 26, "User Cache"),
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
	CREDIT_CHECK_RUN_STATUS_OPEN (DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, 101, "Open"),
	CREDIT_CHECK_RUN_STATUS_COMPLETED (DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, 103, "Completed"),
	CREDIT_CHECK_RUN_STATUS_FAILED (DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, 104, "Failed"),
	PRICE_TYPE_SPOT (DefaultReferenceType.PRICE_TYPE, 105, "Spot Price"),
	PRICE_TYPE_FORWARD (DefaultReferenceType.PRICE_TYPE, 106, "Forward Price"),
	AVERAGING_RULES_SAMPLE1 (DefaultReferenceType.AVERAGING_RULE, 107, "Sample 1 Averaging Rule"),
	AVERAGING_RULES_SAMPLE2 (DefaultReferenceType.AVERAGING_RULE, 108, "Sample 2 Averaging Rule"),	
	STOP_TRIGGER_TYPE_SAMPLE1 (DefaultReferenceType.STOP_TRIGGER_TYPE, 109, "Sample 1 Stop Trigger Type"),	
	STOP_TRIGGER_TYPE_SAMPLE2 (DefaultReferenceType.STOP_TRIGGER_TYPE, 110, "Sample 2 Stop Trigger Type"),
	LIMIT_ORDER_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 111, "Transition for Limit Orders"),
	REFERENCE_ORDER_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 112, "Transition for Reference Orders"),
	ORDER_STATUS_PARTIAL_CANCELLED (DefaultReferenceType.ORDER_STATUS_NAME, 113, "Partially Filled / Cancelled"),
	CREDIT_CHECK_OUTCOME_PASSED (DefaultReferenceType.CREDIT_CHECK_OUTCOME, 114, "Passed"),
	CREDIT_CHECK_OUTCOME_AR_FAILED (DefaultReferenceType.CREDIT_CHECK_OUTCOME, 115, "AR Failed"),
	CREDIT_CHECK_OUTCOME_FAILED (DefaultReferenceType.CREDIT_CHECK_OUTCOME, 116, "Failed"),
	CREDIT_CHECK_RUN_STATUS_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 117, "Transition for Credit Check Run Status"),
	PORTFOLIO_UK_FX (DefaultReferenceType.PORTFOLIO, 118, "UK FX", 20001),
	PORTFOLIO_UK_COPPER (DefaultReferenceType.PORTFOLIO, 119, "UK Copper", 20027),
	PORTFOLIO_UK_GOLD (DefaultReferenceType.PORTFOLIO, 120, "UK Gold", 20028),
	PORTFOLIO_UK_IRIDIUM (DefaultReferenceType.PORTFOLIO, 121, "UK Iridium", 20029),
	PORTFOLIO_UK_LOAN_RENT (DefaultReferenceType.PORTFOLIO, 122, "UK Loan-Rent", 20030),
	PORTFOLIO_UK_OSMIUM (DefaultReferenceType.PORTFOLIO, 123, "UK Osmium", 20031),
	PORTFOLIO_UK_FEES (DefaultReferenceType.PORTFOLIO, 124, "UK Fees", 20032),
	PORTFOLIO_UK_PHYSICAL (DefaultReferenceType.PORTFOLIO, 125, "UK Physical", 20033),
	PORTFOLIO_UK_PLATINUM (DefaultReferenceType.PORTFOLIO, 126, "UK Platinum", 20034),
	PORTFOLIO_UK_RHODIUM (DefaultReferenceType.PORTFOLIO, 127, "UK Rhodium", 20035),
	PORTFOLIO_UK_RUTHENIUM (DefaultReferenceType.PORTFOLIO, 128, "UK Ruthenium", 20036),
	PORTFOLIO_UK_PALLADIUM (DefaultReferenceType.PORTFOLIO, 129, "UK Palladium", 20037),
	PORTFOLIO_UK_ZINC (DefaultReferenceType.PORTFOLIO, 130, "UK Zinc", 20038),
	PORTFOLIO_UK_SILVER (DefaultReferenceType.PORTFOLIO, 131, "UK Silver", 20039),
	PORTFOLIO_UK_GAINS_AND_LOSSES (DefaultReferenceType.PORTFOLIO, 132, "UK Gains & Losses", 20050),
	PORTFOLIO_UK_MIGRATION (DefaultReferenceType.PORTFOLIO, 133, "UK Migration", 20053),
	PORTFOLIO_UK_UNDHEDGED (DefaultReferenceType.PORTFOLIO, 134, "UK Unhedged", 20056),
	PORTFOLIO_UK_AVERAGING (DefaultReferenceType.PORTFOLIO, 135, "UK Averaging", 20057),
	PORTFOLIO_UK_PHYSICAL_OFFSET (DefaultReferenceType.PORTFOLIO, 136, "UK Physical-Offset", 20058),
	PORTFOLIO_UK_NICKEL (DefaultReferenceType.PORTFOLIO, 137, "UK Nickel", 20078),
	PORTFOLIO_US_FEES (DefaultReferenceType.PORTFOLIO, 138, "US Fees", 20026),
	PORTFOLIO_US_GOLD (DefaultReferenceType.PORTFOLIO, 139, "US Gold", 20040),
	PORTFOLIO_US_IRIDIUM (DefaultReferenceType.PORTFOLIO, 140, "US Iridium", 20041),
	PORTFOLIO_US_LOAN_RENT (DefaultReferenceType.PORTFOLIO, 141, "US Loan-Rent", 20042),
	PORTFOLIO_US_OSMIUM (DefaultReferenceType.PORTFOLIO, 142, "US Osmium", 20043),
	PORTFOLIO_US_PALLADIUM (DefaultReferenceType.PORTFOLIO, 143, "US Palladium", 20044),
	PORTFOLIO_US_PHYSICAL (DefaultReferenceType.PORTFOLIO, 144, "US Physical", 20045),
	PORTFOLIO_US_PLATINUM (DefaultReferenceType.PORTFOLIO, 145, "US Platinum", 20046),
	PORTFOLIO_US_RHODIUM (DefaultReferenceType.PORTFOLIO, 146, "US Rhodium", 20047),
	PORTFOLIO_US_RUTHENIUM (DefaultReferenceType.PORTFOLIO, 147, "US Ruthenium", 20048),
	PORTFOLIO_US_SILVER (DefaultReferenceType.PORTFOLIO, 148, "US Silver", 20049),
	PORTFOLIO_US_GAINS_AND_LOSSES (DefaultReferenceType.PORTFOLIO, 149, "US Gains&Losses", 20052),
	PORTFOLIO_US_UNHEDGED (DefaultReferenceType.PORTFOLIO, 150, "US Unhedged", 20059),
	PORTFOLIO_US_AVERAGING (DefaultReferenceType.PORTFOLIO, 151, "US Averaging", 20062),
	PORTFOLIO_US_PHYSICAL_OFFSET (DefaultReferenceType.PORTFOLIO, 152, "US Physical-Offset", 20065), 
	ORDER_STATUS_REJECTED (DefaultReferenceType.ORDER_STATUS_NAME, 153, "Rejected"),
	ORDER_STATUS_PART_FILLED (DefaultReferenceType.ORDER_STATUS_NAME, 154, "Partially Filled"),
	ORDER_STATUS_PART_EXPIRED (DefaultReferenceType.ORDER_STATUS_NAME, 155, "Partially Filled / Expired"),
	ORDER_STATUS_EXPIRED (DefaultReferenceType.ORDER_STATUS_NAME, 156, "Expired"),
	ORDER_STATUS_MATURED (DefaultReferenceType.ORDER_STATUS_NAME, 157, "Matured"),
	PRICE_TYPE_SPOT_PLUS (DefaultReferenceType.PRICE_TYPE, 159, "Spot+ Market Swap Rate"),
	CACHE_EXPIRATION_STATUS (DefaultReferenceType.CACHE_TYPE, 160, "Expiration Status Cache"),	
	DELETION_FLAG_NOT_DELETED (DefaultReferenceType.DELETION_FLAG, 161, "Not Deleted"),	
	DELETION_FLAG_DELETED (DefaultReferenceType.DELETION_FLAG, 162, "Deleted"),	
	ORDER_STATUS_PULLED (DefaultReferenceType.ORDER_STATUS_NAME, 163, "Pulled"),
	METAL_FORM_INGOT (DefaultReferenceType.METAL_FORM , 164, "Ingot"),
	METAL_FORM_SPONGE (DefaultReferenceType.METAL_FORM , 165, "Sponge"),
	METAL_FORM_GRAIN (DefaultReferenceType.METAL_FORM , 166, "Grain"),
	METAL_FORM_NONE (DefaultReferenceType.METAL_FORM , 167, "None"),
	METAL_LOCATION_GERMISTAN_SA(DefaultReferenceType.METAL_LOCATION , 168, "Germiston SA", 1),
	METAL_LOCATION_HK (DefaultReferenceType.METAL_LOCATION , 169, "Hong Kong", 2),
	METAL_LOCATION_LONDON_PLATE (DefaultReferenceType.METAL_LOCATION , 170, "London Plate", 8),
	METAL_LOCATION_NONE (DefaultReferenceType.METAL_LOCATION , 171, "None", 9),
	METAL_LOCATION_ROYSTON (DefaultReferenceType.METAL_LOCATION , 172, "Royston", 10),
	METAL_LOCATION_TANAKA_JAPAN (DefaultReferenceType.METAL_LOCATION , 173, "Tanaka Japan", 11),
	METAL_LOCATION_TANAKA_UMICORE_BELGIUM (DefaultReferenceType.METAL_LOCATION , 174, "Umicore Belgium", 12),
	METAL_LOCATION_VALE (DefaultReferenceType.METAL_LOCATION , 175, "Vale", 13),
	METAL_LOCATION_VALLEY_FORGE (DefaultReferenceType.METAL_LOCATION , 176, "Valley Forge", 14),
	METAL_LOCATION_ZURICH (DefaultReferenceType.METAL_LOCATION , 177, "Zurich", 15),
	METAL_LOCATION_SHANGHAI (DefaultReferenceType.METAL_LOCATION , 178, "Shanghai", 16),
	METAL_LOCATION_BRANDENBERGER (DefaultReferenceType.METAL_LOCATION , 179, "Brandenberger", 17),
	METAL_LOCATION_LME (DefaultReferenceType.METAL_LOCATION , 180, "LME", 7),
	METAL_LOCATION_BRINKS (DefaultReferenceType.METAL_LOCATION , 181, "Brinks", 0),
	METAL_LOCATION_CNT (DefaultReferenceType.METAL_LOCATION , 182, "CNT", 18),
	METAL_LOCATION_UMICORE_GERMANY (DefaultReferenceType.METAL_LOCATION , 183, "Umicore Germany", 19),	
	;
	
	private final ReferenceTo ref;
	private final ReferenceTypeTo refType;
	
	private DefaultReference (DefaultReferenceType type, long id, String name) {
		this.refType = type.getEntity();
		ref = ImmutableReferenceTo.builder()
				.id(id)
				.name(name)
				.endurId(null)
				.idType(type.getEntity().id())
				.build();
	}
	
	private DefaultReference (DefaultReferenceType type, long id, String name, long endurId) {
		this.refType = type.getEntity();
		ref = ImmutableReferenceTo.builder()
				.id(id)
				.name(name)
				.endurId(endurId)
				.idType(type.getEntity().id())
				.build();
	}

	public ReferenceTo getEntity() {
		return ref;
	}

	public ReferenceTypeTo getRefType() {
		return refType;
	}

	public static List<ReferenceTo> asList () {
		return Arrays.asList(DefaultReference.values())
				.stream().map(DefaultReference::getEntity).collect(Collectors.toList());
	}

	public static List<ReferenceTo> asListByType (DefaultReferenceType type) {
		return Arrays.asList(DefaultReference.values())
				.stream()
				.filter(x -> x.refType.equals(type.getEntity()))
				.map(DefaultReference::getEntity).collect(Collectors.toList());
	}
	
	public static Optional<ReferenceTo> findById(long refId) {
		List<ReferenceTo> filtered = asList().stream().filter(x -> x.id() == refId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered.get(0));
		}
	}

	public static Optional<List<ReferenceTo>> findByTypeId(long typeId) {
		List<ReferenceTo> filtered = asList().stream().filter(x -> x.idType() == typeId).collect(Collectors.toList());
		if (filtered.size() == 0) {
			return Optional.empty();
		} else {
			return Optional.of(filtered);
		}
	}	
}

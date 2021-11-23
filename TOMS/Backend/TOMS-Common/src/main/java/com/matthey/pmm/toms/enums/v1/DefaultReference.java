package com.matthey.pmm.toms.enums.v1;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

public enum DefaultReference {
	PARTY_TYPE_INTERNAL_BUNIT(DefaultReferenceType.PARTY_TYPE, 1, "Internal Business Unit", 1000l),
	PARTY_TYPE_EXTERNAL_BUNIT(DefaultReferenceType.PARTY_TYPE, 2, "External Business Unit", 2000l),
	USER_ROLE_PMM_USER (DefaultReferenceType.USER_ROLE, 3, "PMM User", 10000l),
	USER_ROLE_PMM_TRADER (DefaultReferenceType.USER_ROLE, 4, "PMM Trader", 11000l),
	USER_ROLE_ADMIN (DefaultReferenceType.USER_ROLE, 5, "Admin", 12000l),
	USER_ROLE_SERVICE_USER (DefaultReferenceType.USER_ROLE, 6, "Service User", 13000l),
	ORDER_STATUS_PENDING (DefaultReferenceType.ORDER_STATUS_NAME, 7, "Pending", 20000l),
	ORDER_STATUS_FILLED (DefaultReferenceType.ORDER_STATUS_NAME, 9, "Filled", 21000l),
	ORDER_STATUS_CANCELLED (DefaultReferenceType.ORDER_STATUS_NAME, 10, "Cancelled", 22000l),
	ORDER_STATUS_WAITING_APPROVAL (DefaultReferenceType.ORDER_STATUS_NAME, 11, "Waiting Approval", 23000l),
	ORDER_STATUS_CONFIRMED (DefaultReferenceType.ORDER_STATUS_NAME, 12, "Confirmed", 24000l),
	ORDER_TYPE_LIMIT_ORDER (DefaultReferenceType.ORDER_TYPE_NAME, 13, "Limit Order", 30000l),
	ORDER_TYPE_REFERENCE_ORDER (DefaultReferenceType.ORDER_TYPE_NAME, 14, "Reference Order", 31000l),
	BUY_SELL_BUY (DefaultReferenceType.BUY_SELL, 15, "Buy", 40000l),
	BUY_SELL_SELL (DefaultReferenceType.BUY_SELL, 16, "Sell", 41000l),
	EXPIRATION_STATUS_ACTIVE (DefaultReferenceType.EXPIRATION_STATUS, 17, "Active", 50000l),
	EXPIRATION_STATUS_EXPIRED (DefaultReferenceType.EXPIRATION_STATUS, 18, "Expired", 50000l),
	PARTY_TYPE_INTERNAL_LE(DefaultReferenceType.PARTY_TYPE, 19, "Internal Legal Entity", 3000l),
	PARTY_TYPE_EXTERNAL_LE(DefaultReferenceType.PARTY_TYPE, 20, "External Legal Entity", 4000l),
	CACHE_TYPE_PARTY_TYPE (DefaultReferenceType.CACHE_TYPE, 21, "Party Cache", null),
	CACHE_TYPE_USER_ROLE (DefaultReferenceType.CACHE_TYPE, 22, "User Role Cache", null),
	CACHE_TYPE_ORDER_STATUS (DefaultReferenceType.CACHE_TYPE, 23, "Order Status Cache", null),
	CACHE_TYPE_ORDER_TYPE (DefaultReferenceType.CACHE_TYPE, 24, "Order Type Cache", null),
	CACHE_TYPE_BUY_SELL (DefaultReferenceType.CACHE_TYPE, 25, "Buy/Sell Cache", null),
	CACHE_TYPE_USER (DefaultReferenceType.CACHE_TYPE, 26, "User Cache", null),
	QUANTITY_TOZ (DefaultReferenceType.QUANTITY_UNIT, 28, "TOz", 55, 60000l),
	QUANTITY_MT (DefaultReferenceType.QUANTITY_UNIT, 29, "MT", 13, 61000l),
	QUANTITY_GMS (DefaultReferenceType.QUANTITY_UNIT, 30, "gms", 51, 62000l),
	QUANTITY_KGS (DefaultReferenceType.QUANTITY_UNIT, 31, "kgs", 52, 63000l),
	QUANTITY_LBS (DefaultReferenceType.QUANTITY_UNIT, 32, "lbs", 53, 64000l),
	QUANTITY_MGS (DefaultReferenceType.QUANTITY_UNIT, 33, "mgs", 54, 65000l),
	METAL_XRU (DefaultReferenceType.CCY_METAL, 34, "XRU", 63, 70000l),
	METAL_XOS (DefaultReferenceType.CCY_METAL, 35, "XOS", 62, 71000l),
	METAL_XIR (DefaultReferenceType.CCY_METAL, 36, "XIR", 61, 72000l),
	METAL_XAG (DefaultReferenceType.CCY_METAL, 37, "XAG", 53, 73000l),
	METAL_XPT (DefaultReferenceType.CCY_METAL, 38, "XPT", 56, 74000l),
	METAL_XRH (DefaultReferenceType.CCY_METAL, 39, "XRH", 58, 75000l),
	METAL_XAU (DefaultReferenceType.CCY_METAL, 40, "XAU", 54, 76000l),
	METAL_XPD (DefaultReferenceType.CCY_METAL, 41, "XPD", 55, 77000l),
	CCY_USD (DefaultReferenceType.CCY_CURRENCY, 42, "USD", 0, 90000l),
	CCY_EUR (DefaultReferenceType.CCY_CURRENCY, 43, "EUR", 51, 91000l),
	CCY_CNY (DefaultReferenceType.CCY_CURRENCY, 44, "CNY", 60, 92000l),
	CCY_HKD (DefaultReferenceType.CCY_CURRENCY, 45, "HKD", 59, 93000l),
	CCY_ZAR (DefaultReferenceType.CCY_CURRENCY, 46, "ZAR", 57, 94000l),
	CCY_CHF (DefaultReferenceType.CCY_CURRENCY, 47, "CHF", 64, 95000l),
	CCY_GBP (DefaultReferenceType.CCY_CURRENCY, 48, "GBP", 52, 96000l),
	INDEX_NAME_PX_XAG_CNY (DefaultReferenceType.INDEX_NAME, 49, "PX_XAG.CNY", 120000l),
	INDEX_NAME_PX_XAG_EUR (DefaultReferenceType.INDEX_NAME, 50, "PX_XAG.EUR", 121000l),
	INDEX_NAME_PX_XAG_GBP (DefaultReferenceType.INDEX_NAME, 51, "PX_XAG.GBP", 122000l),
	INDEX_NAME_PX_XAG_USD (DefaultReferenceType.INDEX_NAME, 52, "PX_XAG.USD", 123000l),
	INDEX_NAME_PX_XAU_CNY (DefaultReferenceType.INDEX_NAME, 53, "PX_XAU.CNY", 124000l),
	INDEX_NAME_PX_XAU_EUR (DefaultReferenceType.INDEX_NAME, 54, "PX_XAU.EUR", 122500l),
	INDEX_NAME_PX_XAU_GBP (DefaultReferenceType.INDEX_NAME, 55, "PX_XAU.GBP", 122600l),
	INDEX_NAME_PX_XAU_USD (DefaultReferenceType.INDEX_NAME, 56, "PX_XAU.USD", 122700l),
	INDEX_NAME_PX_XIR_CNY (DefaultReferenceType.INDEX_NAME, 57, "PX_XIR.CNY", 122800l),
	INDEX_NAME_PX_XIR_EUR (DefaultReferenceType.INDEX_NAME, 58, "PX_XIR.EUR", 122900l),
	INDEX_NAME_PX_XIR_GBP (DefaultReferenceType.INDEX_NAME, 59, "PX_XIR.GBP", 130000l),
	INDEX_NAME_PX_XIR_USD (DefaultReferenceType.INDEX_NAME, 60, "PX_XIR.USD", 131000l),
	INDEX_NAME_PX_XOS_CNY (DefaultReferenceType.INDEX_NAME, 61, "PX_XOS.CNY", 132000l),
	INDEX_NAME_PX_XOS_EUR (DefaultReferenceType.INDEX_NAME, 62, "PX_XOS.EUR", 133000l),
	INDEX_NAME_PX_XOS_GBP (DefaultReferenceType.INDEX_NAME, 63, "PX_XOS.GBP", 134000l),
	INDEX_NAME_PX_XOS_USD (DefaultReferenceType.INDEX_NAME, 64, "PX_XOS.USD", 135000l),
	INDEX_NAME_PX_XPD_CNY (DefaultReferenceType.INDEX_NAME, 65, "PX_XPD.CNY", 136000l),
	INDEX_NAME_PX_XPD_EUR (DefaultReferenceType.INDEX_NAME, 66, "PX_XPD.EUR", 137000l),
	INDEX_NAME_PX_XPD_GBP (DefaultReferenceType.INDEX_NAME, 67, "PX_XPD.GBP", 138000l),
	INDEX_NAME_PX_XPD_USD (DefaultReferenceType.INDEX_NAME, 68, "PX_XPD.USD", 139000l),
	INDEX_NAME_PX_XPT_CNY (DefaultReferenceType.INDEX_NAME, 69, "PX_XPT.CNY", 140000l),
	INDEX_NAME_PX_XPT_EUR (DefaultReferenceType.INDEX_NAME, 70, "PX_XPT.EUR", 141000l),
	INDEX_NAME_PX_XPT_GBP (DefaultReferenceType.INDEX_NAME, 71, "PX_XPT.GBP", 142000l),
	INDEX_NAME_PX_XPT_USD (DefaultReferenceType.INDEX_NAME, 72, "PX_XPT.USD", 143000l),
	INDEX_NAME_PX_XRH_CNY (DefaultReferenceType.INDEX_NAME, 73, "PX_XRH.CNY", 144000l),
	INDEX_NAME_PX_XRH_EUR (DefaultReferenceType.INDEX_NAME, 74, "PX_XRH.EUR", 145000l),
	INDEX_NAME_PX_XRH_GBP (DefaultReferenceType.INDEX_NAME, 75, "PX_XRH.GBP", 146000l),
	INDEX_NAME_PX_XRH_USD (DefaultReferenceType.INDEX_NAME, 76, "PX_XRH.USD", 147000l),
	INDEX_NAME_PX_XRU_CNY (DefaultReferenceType.INDEX_NAME, 77, "PX_XRU.CNY", 148000l),
	INDEX_NAME_PX_XRU_EUR (DefaultReferenceType.INDEX_NAME, 78, "PX_XRU.EUR", 149000l),
	INDEX_NAME_PX_XRU_GBP (DefaultReferenceType.INDEX_NAME, 79, "PX_XRU.GBP", 150000l),
	INDEX_NAME_PX_XRU_USD (DefaultReferenceType.INDEX_NAME, 80, "PX_XRU.USD", 151000l),
	INDEX_NAME_FX_EUR_CHF (DefaultReferenceType.INDEX_NAME, 81, "FX_EUR.CHF", 152000l),
	INDEX_NAME_FX_EUR_CNY (DefaultReferenceType.INDEX_NAME, 82, "FX_EUR.CNY", 153000l),
	INDEX_NAME_FX_EUR_GBP (DefaultReferenceType.INDEX_NAME, 83, "FX_EUR.GBP", 154000l),
	INDEX_NAME_FX_EUR_HKD (DefaultReferenceType.INDEX_NAME, 84, "FX_EUR.HKD", 155000l),
	INDEX_NAME_FX_EUR_USD (DefaultReferenceType.INDEX_NAME, 85, "FX_EUR.USD", 156000l),
	INDEX_NAME_FX_EUR_ZAR (DefaultReferenceType.INDEX_NAME, 86, "FX_EUR.ZAR", 157000l),
	INDEX_NAME_FX_GBP_CHF (DefaultReferenceType.INDEX_NAME, 87, "FX_GBP.CHF", 158000l),
	INDEX_NAME_FX_GBP_CNY (DefaultReferenceType.INDEX_NAME, 88, "FX_GBP.CNY", 159000l),
	INDEX_NAME_FX_GBP_HKD (DefaultReferenceType.INDEX_NAME, 89, "FX_GBP.HKD", 160000l),
	INDEX_NAME_FX_GBP_USD (DefaultReferenceType.INDEX_NAME, 90, "FX_GBP.USD", 161000l),
	INDEX_NAME_FX_GBP_ZAR (DefaultReferenceType.INDEX_NAME, 91, "FX_GBP.ZAR", 162000l),
	INDEX_NAME_FX_HKD_USD (DefaultReferenceType.INDEX_NAME, 92, "FX_HKD.USD", 163000l),
	INDEX_NAME_FX_USD_CHF (DefaultReferenceType.INDEX_NAME, 93, "FX_USD.CHF", 164000l),
	INDEX_NAME_FX_USD_CNY (DefaultReferenceType.INDEX_NAME, 94, "FX_USD.CNY", 165000l),
	INDEX_NAME_FX_USD_HKD (DefaultReferenceType.INDEX_NAME, 95, "FX_USD.HKD", 166000l),
	INDEX_NAME_FX_USD_ZAR (DefaultReferenceType.INDEX_NAME, 96, "FX_USD.ZAR", 167000l),
	YES_NO_YES (DefaultReferenceType.YES_NO, 97, "Yes", 1, 180000l),
	YES_NO_NO (DefaultReferenceType.YES_NO, 98, "No", 0, 181000l),	
	PAYMENT_PERIOD_SAMPLE1 (DefaultReferenceType.PAYMENT_PERIOD, 99, "Sample 1 Payment Period", 185000l), // Endur Side ID?
	PAYMENT_PERIOD_SAMPLE2 (DefaultReferenceType.PAYMENT_PERIOD, 100, "Sample 2 Payment Period", 186000l),
	CREDIT_CHECK_RUN_STATUS_OPEN (DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, 101, "Open", 190000l),
	CREDIT_CHECK_RUN_STATUS_COMPLETED (DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, 103, "Completed", 191000l),
	CREDIT_CHECK_RUN_STATUS_FAILED (DefaultReferenceType.CREDIT_CHECK_RUN_STATUS, 104, "Failed", 192000l),
	PRICE_TYPE_SPOT (DefaultReferenceType.PRICE_TYPE, 105, "Spot Price", 200000l),
	PRICE_TYPE_FORWARD (DefaultReferenceType.PRICE_TYPE, 106, "Forward Price", 201000l),
	AVERAGING_RULES_SAMPLE1 (DefaultReferenceType.AVERAGING_RULE, 107, "Sample 1 Averaging Rule", 210000l),
	AVERAGING_RULES_SAMPLE2 (DefaultReferenceType.AVERAGING_RULE, 108, "Sample 2 Averaging Rule", 211000l),	
	STOP_TRIGGER_TYPE_SAMPLE1 (DefaultReferenceType.STOP_TRIGGER_TYPE, 109, "Sample 1 Stop Trigger Type", 220000l),	
	STOP_TRIGGER_TYPE_SAMPLE2 (DefaultReferenceType.STOP_TRIGGER_TYPE, 110, "Sample 2 Stop Trigger Type", 221000l),
	LIMIT_ORDER_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 111, "Transition for Limit Orders", 230000l),
	REFERENCE_ORDER_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 112, "Transition for Reference Orders", 231000l),
	ORDER_STATUS_PARTIAL_CANCELLED (DefaultReferenceType.ORDER_STATUS_NAME, 113, "Partially Filled / Cancelled", 240000l),
	CREDIT_CHECK_OUTCOME_PASSED (DefaultReferenceType.CREDIT_CHECK_OUTCOME, 114, "Passed", 25000l),
	CREDIT_CHECK_OUTCOME_AR_FAILED (DefaultReferenceType.CREDIT_CHECK_OUTCOME, 115, "AR Failed", 251000l),
	CREDIT_CHECK_OUTCOME_FAILED (DefaultReferenceType.CREDIT_CHECK_OUTCOME, 116, "Failed", 252000l),
	CREDIT_CHECK_RUN_STATUS_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 117, "Transition for Credit Check Run Status", 232000l),
	PORTFOLIO_UK_FX (DefaultReferenceType.PORTFOLIO, 118, "UK FX", 260000l),
	PORTFOLIO_UK_COPPER (DefaultReferenceType.PORTFOLIO, 119, "UK Copper", 20027, 261000l),
	PORTFOLIO_UK_GOLD (DefaultReferenceType.PORTFOLIO, 120, "UK Gold", 20028, 262000l),
	PORTFOLIO_UK_IRIDIUM (DefaultReferenceType.PORTFOLIO, 121, "UK Iridium", 20029, 263000l),
	PORTFOLIO_UK_LOAN_RENT (DefaultReferenceType.PORTFOLIO, 122, "UK Loan-Rent", 20030, 264000l),
	PORTFOLIO_UK_OSMIUM (DefaultReferenceType.PORTFOLIO, 123, "UK Osmium", 20031, 265000l),
	PORTFOLIO_UK_FEES (DefaultReferenceType.PORTFOLIO, 124, "UK Fees", 20032, 266000l),
	PORTFOLIO_UK_PHYSICAL (DefaultReferenceType.PORTFOLIO, 125, "UK Physical", 20033, 267000l),
	PORTFOLIO_UK_PLATINUM (DefaultReferenceType.PORTFOLIO, 126, "UK Platinum", 20034, 268000l),
	PORTFOLIO_UK_RHODIUM (DefaultReferenceType.PORTFOLIO, 127, "UK Rhodium", 20035, 269000l),
	PORTFOLIO_UK_RUTHENIUM (DefaultReferenceType.PORTFOLIO, 128, "UK Ruthenium", 20036, 270000l),
	PORTFOLIO_UK_PALLADIUM (DefaultReferenceType.PORTFOLIO, 129, "UK Palladium", 20037, 271000l),
	PORTFOLIO_UK_ZINC (DefaultReferenceType.PORTFOLIO, 130, "UK Zinc", 20038, 272000l),
	PORTFOLIO_UK_SILVER (DefaultReferenceType.PORTFOLIO, 131, "UK Silver", 20039, 272000l),
	PORTFOLIO_UK_GAINS_AND_LOSSES (DefaultReferenceType.PORTFOLIO, 132, "UK Gains & Losses", 20050, 273000l),
	PORTFOLIO_UK_MIGRATION (DefaultReferenceType.PORTFOLIO, 133, "UK Migration", 20053, 274000l),
	PORTFOLIO_UK_UNDHEDGED (DefaultReferenceType.PORTFOLIO, 134, "UK Unhedged", 20056, 275000l),
	PORTFOLIO_UK_AVERAGING (DefaultReferenceType.PORTFOLIO, 135, "UK Averaging", 20057, 275000l),
	PORTFOLIO_UK_PHYSICAL_OFFSET (DefaultReferenceType.PORTFOLIO, 136, "UK Physical-Offset", 20058, 276000l),
	PORTFOLIO_UK_NICKEL (DefaultReferenceType.PORTFOLIO, 137, "UK Nickel", 20078, 2770000l),
	PORTFOLIO_US_FEES (DefaultReferenceType.PORTFOLIO, 138, "US Fees", 20026, 278000l),
	PORTFOLIO_US_GOLD (DefaultReferenceType.PORTFOLIO, 139, "US Gold", 20040, 279000l),
	PORTFOLIO_US_IRIDIUM (DefaultReferenceType.PORTFOLIO, 140, "US Iridium", 20041, 280000l),
	PORTFOLIO_US_LOAN_RENT (DefaultReferenceType.PORTFOLIO, 141, "US Loan-Rent", 20042, 281000l),
	PORTFOLIO_US_OSMIUM (DefaultReferenceType.PORTFOLIO, 142, "US Osmium", 20043, 282000l),
	PORTFOLIO_US_PALLADIUM (DefaultReferenceType.PORTFOLIO, 143, "US Palladium", 20044, 283000l),
	PORTFOLIO_US_PHYSICAL (DefaultReferenceType.PORTFOLIO, 144, "US Physical", 20045, 284000l),
	PORTFOLIO_US_PLATINUM (DefaultReferenceType.PORTFOLIO, 145, "US Platinum", 20046, 285000l),
	PORTFOLIO_US_RHODIUM (DefaultReferenceType.PORTFOLIO, 146, "US Rhodium", 20047, 286000l),
	PORTFOLIO_US_RUTHENIUM (DefaultReferenceType.PORTFOLIO, 147, "US Ruthenium", 20048, 287000l),
	PORTFOLIO_US_SILVER (DefaultReferenceType.PORTFOLIO, 148, "US Silver", 20049, 288000l),
	PORTFOLIO_US_GAINS_AND_LOSSES (DefaultReferenceType.PORTFOLIO, 149, "US Gains&Losses", 20052, 289000l),
	PORTFOLIO_US_UNHEDGED (DefaultReferenceType.PORTFOLIO, 150, "US Unhedged", 20059, 290000l),
	PORTFOLIO_US_AVERAGING (DefaultReferenceType.PORTFOLIO, 151, "US Averaging", 20062, 291000l),
	PORTFOLIO_US_PHYSICAL_OFFSET (DefaultReferenceType.PORTFOLIO, 152, "US Physical-Offset", 20065, 292000l), 
	ORDER_STATUS_REJECTED (DefaultReferenceType.ORDER_STATUS_NAME, 153, "Rejected", 241000l),
	ORDER_STATUS_PART_FILLED (DefaultReferenceType.ORDER_STATUS_NAME, 154, "Partially Filled", 242000l),
	ORDER_STATUS_PART_EXPIRED (DefaultReferenceType.ORDER_STATUS_NAME, 155, "Partially Filled / Expired", 243000l),
	ORDER_STATUS_EXPIRED (DefaultReferenceType.ORDER_STATUS_NAME, 156, "Expired", 243000l),
	ORDER_STATUS_MATURED (DefaultReferenceType.ORDER_STATUS_NAME, 157, "Matured", 244000l),
	PRICE_TYPE_SPOT_PLUS (DefaultReferenceType.PRICE_TYPE, 159, "Spot+ Market Swap Rate", null),
	CACHE_EXPIRATION_STATUS (DefaultReferenceType.CACHE_TYPE, 160, "Expiration Status Cache", null),	
	DELETION_FLAG_NOT_DELETED (DefaultReferenceType.DELETION_FLAG, 161, "Not Deleted", null),	
	DELETION_FLAG_DELETED (DefaultReferenceType.DELETION_FLAG, 162, "Deleted", 300000l),	
	ORDER_STATUS_PULLED (DefaultReferenceType.ORDER_STATUS_NAME, 163, "Pulled", 245000l),
	METAL_FORM_INGOT (DefaultReferenceType.METAL_FORM , 164, "Ingot", 310000l),
	METAL_FORM_SPONGE (DefaultReferenceType.METAL_FORM , 165, "Sponge", 311000l),
	METAL_FORM_GRAIN (DefaultReferenceType.METAL_FORM , 166, "Grain", 312000l),
	METAL_FORM_NONE (DefaultReferenceType.METAL_FORM , 167, "None", 313000l),
	METAL_LOCATION_GERMISTAN_SA(DefaultReferenceType.METAL_LOCATION , 168, "Germiston SA", 1, 320000l),
	METAL_LOCATION_HK (DefaultReferenceType.METAL_LOCATION , 169, "Hong Kong", 2, 321000l),
	METAL_LOCATION_LONDON_PLATE (DefaultReferenceType.METAL_LOCATION , 170, "London Plate", 8, 322000l),
	METAL_LOCATION_NONE (DefaultReferenceType.METAL_LOCATION , 171, "None", 9, 323000l),
	METAL_LOCATION_ROYSTON (DefaultReferenceType.METAL_LOCATION , 172, "Royston", 10, 324000l),
	METAL_LOCATION_TANAKA_JAPAN (DefaultReferenceType.METAL_LOCATION , 173, "Tanaka Japan", 11, 325000l),
	METAL_LOCATION_TANAKA_UMICORE_BELGIUM (DefaultReferenceType.METAL_LOCATION , 174, "Umicore Belgium", 12, 325000l),
	METAL_LOCATION_VALE (DefaultReferenceType.METAL_LOCATION , 175, "Vale", 13, 326000l),
	METAL_LOCATION_VALLEY_FORGE (DefaultReferenceType.METAL_LOCATION , 176, "Valley Forge", 14, 327000l),
	METAL_LOCATION_ZURICH (DefaultReferenceType.METAL_LOCATION , 177, "Zurich", 15, 328000l),
	METAL_LOCATION_SHANGHAI (DefaultReferenceType.METAL_LOCATION , 178, "Shanghai", 16, 329000l),
	METAL_LOCATION_BRANDENBERGER (DefaultReferenceType.METAL_LOCATION , 179, "Brandenberger", 17, 330000l),
	METAL_LOCATION_LME (DefaultReferenceType.METAL_LOCATION , 180, "LME", 7, 331000l),
	METAL_LOCATION_BRINKS (DefaultReferenceType.METAL_LOCATION , 181, "Brinks", 0, 332000l),
	METAL_LOCATION_CNT (DefaultReferenceType.METAL_LOCATION , 182, "CNT", 18, 333000l),
	METAL_LOCATION_UMICORE_GERMANY (DefaultReferenceType.METAL_LOCATION , 183, "Umicore Germany", 19, 334000l),	
	VALIDATION_TYPE_GOOD_TIL_CANCELLED (DefaultReferenceType.VALIDATION_TYPE , 184, "Good Til Cancelled", 340000l),	
	VALIDATION_TYPE_EXPIRY_DATE (DefaultReferenceType.VALIDATION_TYPE , 185, "Expiry Date", 341000l),
	SYMBOLIC_DATE_1D (DefaultReferenceType.SYMBOLIC_DATE , 186, "1d", 350000l),
	SYMBOLIC_DATE_2D (DefaultReferenceType.SYMBOLIC_DATE , 187, "2d", 351000l),
	SYMBOLIC_DATE_EOM (DefaultReferenceType.SYMBOLIC_DATE , 188, "eom", 352000l),
	SYMBOLIC_DATE_SOM (DefaultReferenceType.SYMBOLIC_DATE , 189, "som", 353000l),
	REF_SOURCE_NONE (DefaultReferenceType.REF_SOURCE, 190, "None", 0, 370000l),
	REF_SOURCE_MANUAL (DefaultReferenceType.REF_SOURCE, 191, "Manual", 33, 371000l),
	REF_SOURCE_BLOOMBERG (DefaultReferenceType.REF_SOURCE, 192, "BLOOMBERG", 20001, 372000l),
	REF_SOURCE_LBMA (DefaultReferenceType.REF_SOURCE, 193, "LBMA", 20002, 373000l),
	REF_SOURCE_LPPM (DefaultReferenceType.REF_SOURCE, 194, "LPPM", 20004, 374000l),
	REF_SOURCE_LIBOR_BBA (DefaultReferenceType.REF_SOURCE, 195, "LIBOR-BBA", 20005, 375000l),
	REF_SOURCE_LBMA_PM (DefaultReferenceType.REF_SOURCE, 196, "LBMA PM", 20006, 376000l),
	REF_SOURCE_LBMA_AM (DefaultReferenceType.REF_SOURCE, 197, "LBMA AM", 20007, 377000l),
	REF_SOURCE_JM_NY_OPENING (DefaultReferenceType.REF_SOURCE, 198, "JM NY Opening", 20008, 378000l),
	REF_SOURCE_COMEX (DefaultReferenceType.REF_SOURCE, 199, "COMEX", 20009, 379000l),
	REF_SOURCE_JM_LONDON_OPENING (DefaultReferenceType.REF_SOURCE, 200, "JM London Opening", 20011, 380000l),
	REF_SOURCE_JM_HK_OPENING (DefaultReferenceType.REF_SOURCE, 201, "JM HK Opening", 20012, 381000l),
	REF_SOURCE_JM_HK_CLOSING (DefaultReferenceType.REF_SOURCE, 202, "JM HK Closing", 20013, 382000l),
	REF_SOURCE_ECB (DefaultReferenceType.REF_SOURCE, 203, "ECB", 20014, 383000l),
	REF_SOURCE_CUSTOM (DefaultReferenceType.REF_SOURCE, 204, "Custom", 20016, 384000l),
	REF_SOURCE_LME_AM (DefaultReferenceType.REF_SOURCE, 205, "LME AM", 20017, 385000l),
	REF_SOURCE_LME_PM (DefaultReferenceType.REF_SOURCE, 206, "LME PM", 20018, 386000l),
	REF_SOURCE_COMDAQ (DefaultReferenceType.REF_SOURCE, 207, "Comdaq", 20019, 387000l),
	REF_SOURCE_NY_MW_HIGH (DefaultReferenceType.REF_SOURCE, 208, "NY MW High", 20020, 389000l),
	REF_SOURCE_NY_MW_LOW (DefaultReferenceType.REF_SOURCE, 209, "NY MW Low", 20021, 390000l),
	REF_SOURCE_LBMA_SILVER (DefaultReferenceType.REF_SOURCE, 210, "LBMA Silver", 20022, 391000l),
	REF_SOURCE_LME_BASE (DefaultReferenceType.REF_SOURCE, 211, "LME Base", 20023, 392000l),
	REF_SOURCE_CITIBANK_FORWARD (DefaultReferenceType.REF_SOURCE, 212, "Citibank Forward", 20024, 393000l),
	REF_SOURCE_PHYSICAL (DefaultReferenceType.REF_SOURCE, 213, "Physical", 20025, 394000l),
	REF_SOURCE_CITIBANK (DefaultReferenceType.REF_SOURCE, 214, "Citibank 1500", 20026, 395000l),
	REF_SOURCE_IMPALA_RATE (DefaultReferenceType.REF_SOURCE, 215, "Impala Rate", 20027, 396000l),
	REF_SOURCE_NY_MW_MID (DefaultReferenceType.REF_SOURCE, 216, "NY MW Mid", 20028, 397000l),
	REF_SOURCE_FNB (DefaultReferenceType.REF_SOURCE, 217, "FNB", 20029, 398000l),
	REF_SOURCE_BFIX_1500 (DefaultReferenceType.REF_SOURCE, 218, "BFIX 1500", 20030, 399000l),
	REF_SOURCE_BFIX_FORWARD (DefaultReferenceType.REF_SOURCE, 219, "BFIX Forward", 20031, 400000l),
	REF_SOURCE_BFIX_1400 (DefaultReferenceType.REF_SOURCE, 220, "BFIX 1400", 20032, 401000l),
	REF_SOURCE_BOC (DefaultReferenceType.REF_SOURCE, 221, "BOC", 20033, 402000l),
	REF_SOURCE_JMLO_BFIX_1400 (DefaultReferenceType.REF_SOURCE, 222, "JMLO BFIX 1400", 20034, 403000l),
	REF_SOURCE_JMLO_BFIX_1500 (DefaultReferenceType.REF_SOURCE, 223, "JMLO BFIX 1500", 20035, 404000l),
	CONTRACT_TYPE_REFERENCE_AVERAGE (DefaultReferenceType.CONTRACT_TYPE_REFERENCE_ORDER , 224, "Average", 410000l),
	CONTRACT_TYPE_REFERENCE_FIXING (DefaultReferenceType.CONTRACT_TYPE_REFERENCE_ORDER , 225, "Fixing", 411000l),
	CONTRACT_TYPE_LIMIT_RELATIVE (DefaultReferenceType.CONTRACT_TYPE_LIMIT_ORDER , 226, "Relative", 420000l),
	CONTRACT_TYPE_LIMIT_FIXED (DefaultReferenceType.CONTRACT_TYPE_LIMIT_ORDER , 227, "Fixed", 421000l),
	REFERENCE_ORDER_LEG_TRANSITION (DefaultReferenceType.PROCESS_TRANSITION_TYPE, 228, "Transition for Reference Orders Legs", 233000l),
	ORDER_TYPE_CATEGORY_OPEN (DefaultReferenceType.ORDER_TYPE_CATEGORY, 229, "Open", 430000l),
	ORDER_TYPE_CATEGORY_CLOSED (DefaultReferenceType.ORDER_TYPE_CATEGORY, 230, "Closed", 431000l),
	TICKER_EURXAG (DefaultReferenceType.TICKER, 231, "EUR/XAG", 450000l),
	TICKER_EURXAU (DefaultReferenceType.TICKER, 232, "EUR/XAU", 451000l),
	TICKER_GBPXPT (DefaultReferenceType.TICKER, 233, "GBP/XPT", 452000l),
	TICKER_XAGCNY (DefaultReferenceType.TICKER, 234, "XAG/CNY", 453000l),
	TICKER_XAGEUR (DefaultReferenceType.TICKER, 235, "XAG/EUR", 454000l),
	TICKER_XAGGBP (DefaultReferenceType.TICKER, 236, "XAG/GBP", 455000l),
	TICKER_XAGUSD (DefaultReferenceType.TICKER, 237, "XAG/USD", 456000l),
	TICKER_XAUCNY (DefaultReferenceType.TICKER, 238, "XAU/CNY", 457000l),
	TICKER_XAUEUR (DefaultReferenceType.TICKER, 239, "XAU/EUR", 458000l),
	TICKER_XAUGBP (DefaultReferenceType.TICKER, 240, "XAU/GBP", 459000l),
	TICKER_XAUUSD (DefaultReferenceType.TICKER, 241, "XAU/USD", 460000l),
	TICKER_XIRCNY (DefaultReferenceType.TICKER, 242, "XIR/CNY", 461000l),
	TICKER_XIREUR (DefaultReferenceType.TICKER, 243, "XIR/EUR", 462000l),
	TICKER_XIRGBP (DefaultReferenceType.TICKER, 244, "XIR/GBP", 463000l),
	TICKER_XIRUSD (DefaultReferenceType.TICKER, 245, "XIR/USD", 464000l),
	TICKER_XIRZAR (DefaultReferenceType.TICKER, 246, "XIR/ZAR", 465000l),
	TICKER_XOSCNY (DefaultReferenceType.TICKER, 247, "XOS/CNY", 466000l),
	TICKER_XOSEUR (DefaultReferenceType.TICKER, 248, "XOS/EUR", 467000l),
	TICKER_XOSGBP (DefaultReferenceType.TICKER, 249, "XOS/GBP", 468000l),
	TICKER_XOSUSD (DefaultReferenceType.TICKER, 250, "XOS/USD", 469000l),	
	TICKER_XPDCNY (DefaultReferenceType.TICKER, 251, "XPD/CNY", 470000l),
	TICKER_XPDEUR (DefaultReferenceType.TICKER, 252, "XPD/EUR", 471000l),
	TICKER_XPDGBP (DefaultReferenceType.TICKER, 253, "XPD/GBP", 472000l),
	TICKER_XPDUSD (DefaultReferenceType.TICKER, 254, "XPD/USD", 473000l),
	TICKER_XPDZAR (DefaultReferenceType.TICKER, 255, "XPD/ZAR", 474000l),	
	TICKER_XPTCNY (DefaultReferenceType.TICKER, 256, "XPT/CNY", 475000l),
	TICKER_XPTEUR (DefaultReferenceType.TICKER, 257, "XPT/EUR", 476000l),
	TICKER_XPTGBP (DefaultReferenceType.TICKER, 258, "XPT/GBP", 477000l),
	TICKER_XPTUSD (DefaultReferenceType.TICKER, 259, "XPT/USD", 478000l),
	TICKER_XPTZAR (DefaultReferenceType.TICKER, 260, "XPT/ZAR", 479000l),
	TICKER_XRHCNY (DefaultReferenceType.TICKER, 261, "XRH/CNY", 480000l),
	TICKER_XRHEUR (DefaultReferenceType.TICKER, 262, "XRH/EUR", 481000l),
	TICKER_XRHGBP (DefaultReferenceType.TICKER, 263, "XRH/GBP", 482000l),
	TICKER_XRHUSD (DefaultReferenceType.TICKER, 264, "XRH/USD", 483000l),
	TICKER_XRHZAR (DefaultReferenceType.TICKER, 265, "XRH/ZAR", 484000l),	
	TICKER_XRUCNY (DefaultReferenceType.TICKER, 266, "XRU/CNY", 485000l),
	TICKER_XRUEUR (DefaultReferenceType.TICKER, 267, "XRU/EUR", 486000l),
	TICKER_XRUGBP (DefaultReferenceType.TICKER, 268, "XRU/GBP", 487000l),
	TICKER_XRUUSD (DefaultReferenceType.TICKER, 269, "XRU/USD", 488000l),
	TICKER_XRUZAR (DefaultReferenceType.TICKER, 270, "XRU/ZAR", 489000l),
	TICKER_ZARXPT (DefaultReferenceType.TICKER, 271, "ZAR/XPT", 490000l),
	;
	
	private final ReferenceTo ref;
	private final ReferenceTypeTo refType;
	
	private DefaultReference (DefaultReferenceType type, long id, String name, Long sortColumn) {
		this.refType = type.getEntity();
		ref = ImmutableReferenceTo.builder()
				.id(id)
				.name(name)
				.endurId(null)
				.idType(type.getEntity().id())
				.sortColumn(sortColumn)
				.build();
	}
	
	private DefaultReference (DefaultReferenceType type, long id, String name, long endurId, Long sortColumn) {
		this.refType = type.getEntity();
		ref = ImmutableReferenceTo.builder()
				.id(id)
				.name(name)
				.endurId(endurId)
				.idType(type.getEntity().id())
				.sortColumn(sortColumn)
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

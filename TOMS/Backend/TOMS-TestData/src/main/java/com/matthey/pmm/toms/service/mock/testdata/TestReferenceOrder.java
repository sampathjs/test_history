package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

public enum TestReferenceOrder {
	TEST_ORDER_1A(100003, 1, TestBunit.JM_PMM_UK, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_UK_PLATINUM, null,
			DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, "TEST_ORDER_1A", DefaultReference.METAL_FORM_INGOT, DefaultReference.METAL_LOCATION_ROYSTON,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_REFERENCE_AVERAGE,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2000-01-01 08:00:00", TestUser.ANDREW_BAYNES,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_8),
			Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_1), Arrays.asList(TestOrderComment.TEST_COMMENT_4, TestOrderComment.TEST_COMMENT_5),
			// reference order fields
			 100d, 
			30d, 45d, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_1)
			),
	TEST_ORDER_1B(100003, 2, TestBunit.JM_PMM_UK, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_UK_PLATINUM, null,
			DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, "TEST_ORDER_1B", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_UMICORE_BELGIUM,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_REFERENCE_AVERAGE,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2005-12-23 12:00:00", TestUser.ANDREW_BAYNES,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_8), 
			Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_1), Arrays.asList(TestOrderComment.TEST_COMMENT_7, TestOrderComment.TEST_COMMENT_8),
			// reference order fields
			100d, 30d, 45d, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_1, TestReferenceOrderLeg.TEST_LEG_2)
			),
	TEST_ORDER_2(100004, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_ORDER_2", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_UMICORE_BELGIUM,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, 
			DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING, DefaultReference.TICKER_XRUUSD,
			TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_2, TestCreditCheck.TEST_CREDIT_CHECK_6),  
			Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_1), Arrays.asList(TestOrderComment.TEST_COMMENT_6),
			// reference order fields
			100d, null, 45d, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_9)
			),
	TEST_ORDER_3(100005, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_ORDER_3", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			null, null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_3, TestReferenceOrderLeg.TEST_LEG_4, 
					TestReferenceOrderLeg.TEST_LEG_5, TestReferenceOrderLeg.TEST_LEG_6,
					TestReferenceOrderLeg.TEST_LEG_7, TestReferenceOrderLeg.TEST_LEG_8)
			),
	TEST_IN_STATUS_PENDING(120003, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_IN_STATUS_PENDING", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_10)
			),	
	TEST_IN_STATUS_CONFIRMED(120004, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "REFERENCE_ORDER_CONFIRMED", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_4), null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_11)
			),	
	TEST_IN_STATUS_FILLED(120005, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_IN_STATUS_FILLED", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_FILLED, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_12)
			),	
	TEST_IN_STATUS_PULLED(120006, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_IN_STATUS_PULLED", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_PULLED, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_13)
			),			
	TEST_IN_STATUS_REJECTED(120007, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_IN_STATUS_REJECTED", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_REJECTED, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_14)
			),				
	TEST_FOR_LEG_DELETION(120008, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_IN_STATUS_REJECTED", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_FOR_LEG_DELETION, TestReferenceOrderLeg.MAIN_LEG_FOR_LEG_DELETION)
			),			
	
	TEST_FOR_LEG_DELETION_ALL_LEGS(120009, 1, TestBunit.JM_PMM_US, TestBunit.ANGLO_PLATINUM_MARKETING___BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_IN_STATUS_REJECTED", DefaultReference.METAL_FORM_SPONGE, DefaultReference.METAL_LOCATION_VALLEY_FORGE,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_REFERENCE_FIXING,
			DefaultReference.TICKER_XRUUSD,
			TestUser.ANDREW_BAYNES, "1996-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(), null, null,
			 100d, 
			// reference order fields
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.MAIN_LEG_FOR_LEG_DELETION_ALL_LEGS)
			),		
	;
	
	private ReferenceOrderTo referenceOrder;
	
	private TestReferenceOrder (long orderId, int version, TestBunit internalBu, TestBunit externalBu,
			DefaultReference intPfolio, DefaultReference extPfolio,
			DefaultReference buySell,
			DefaultReference baseCurrency, Double baseQuantity, DefaultReference baseQuantityUnit, 
			DefaultReference termCurrency, String reference, DefaultReference metalForm, 
			DefaultReference metalLocation,			
			DefaultOrderStatus orderStatus, DefaultReference contractType, DefaultReference ticker, 
			TestUser createdBy, String createdAt,
			String lastUpdate, TestUser updatedByUser,  
			List<TestCreditCheck> creditChecks,
			List<TestFill> fills, List<TestOrderComment> orderComments,
			// << order fields
			Double contangoBackwardation, 
		    Double fxRateSpread, Double metalPriceSpread,
		    List<TestReferenceOrderLeg> legs// << reference order fields
			) {
		// order type has to be limit order always
		referenceOrder = ImmutableReferenceOrderTo.builder()
				.idOrderType(DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity().id())
				.id(orderId)
				.version(version)				
				.idInternalBu(internalBu.getEntity().id())
				.idInternalLe(internalBu.getEntity().idLegalEntity())
				.idExternalBu(externalBu.getEntity().id())
				.idExternalLe(externalBu.getEntity().idLegalEntity())
				.idIntPortfolio(intPfolio != null?intPfolio.getEntity().id():null)
				.idExtPortfolio(extPfolio != null?extPfolio.getEntity().id():null)
				.idBuySell (buySell.getEntity().id())
				.idBaseCurrency(baseCurrency.getEntity().id())
				.baseQuantity(baseQuantity)
				.idBaseQuantityUnit(baseQuantityUnit != null?baseQuantityUnit.getEntity().id():null)
				.idTermCurrency(termCurrency.getEntity().id())
				.reference(reference)
				.idMetalForm(metalForm != null?metalForm.getEntity().id():null)
				.idMetalLocation(metalLocation != null?metalLocation.getEntity().id():null)
				.idOrderStatus(orderStatus.getEntity().id())
				.createdAt(createdAt)
				.idCreatedByUser(createdBy.getEntity().id())
				.lastUpdate(lastUpdate)
				.idUpdatedByUser(updatedByUser.getEntity().id()) 
				.idContractType(contractType.getEntity().id())
				.idTicker(ticker.getEntity().id())
				.fillIds (fills!=null?fills.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.creditChecksIds((creditChecks!=null?creditChecks.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
				.orderCommentIds((orderComments!=null?orderComments.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
				 // << order fields
				.idContractType(contractType.getEntity().id())
				.contangoBackwardation(contangoBackwardation)
				.fxRateSpread(fxRateSpread)
				.metalPriceSpread(metalPriceSpread)
				.addAllLegIds((legs!=null?legs.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
				.fillPercentage(0.0d)				
				.build();
	}

	public ReferenceOrderTo getEntity () {
		return referenceOrder;
	}

	public void setEntity (ReferenceOrderTo newReferenceOrder) {
		this.referenceOrder = newReferenceOrder;
	}
	
	public static List<ReferenceOrderTo> asList () {
		return Arrays.asList(TestReferenceOrder.values())
				.stream().map(TestReferenceOrder::getEntity).collect(Collectors.toList());
	}	

	public static List<Long> asListOfIds () {
		return Arrays.asList(TestReferenceOrder.values())
				.stream().map(x -> x.getEntity().id()).collect(Collectors.toList());
	}	

	
	public static List<TestReferenceOrder> asEnumList () {
		return Arrays.asList(TestReferenceOrder.values())
				.stream().collect(Collectors.toList());
	}	
}

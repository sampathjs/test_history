package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;

public enum TestLimitOrder {
	TEST_ORDER_1A(100000, 1, TestParty.JM_PMM_UK_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_UK_FX, null, DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, "TEST_ORDER_1A", DefaultReference.METAL_FORM_INGOT, DefaultReference.METAL_LOCATION_ROYSTON,
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_LIMIT_RELATIVE, DefaultReference.TICKER_XPDEUR, 
			TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2000-01-01 08:00:00", TestUser.ANDREW_BAYNES,
			DefaultReference.PRICE_TYPE_SPOT, null,
			"2000-01-15 16:00:00", "2001-04-28", DefaultReference.SYMBOLIC_DATE_1D, 1200.00d,
			DefaultReference.YES_NO_NO, 1150.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE1, 
			DefaultReference.METAL_XPT, DefaultReference.VALIDATION_TYPE_EXPIRY_DATE, "2010-01-31", 1.0d, null,
			Arrays.asList(TestOrderComment.TEST_COMMENT_1, TestOrderComment.TEST_COMMENT_2)),
	TEST_ORDER_1B(100000, 2, TestParty.JM_PMM_UK_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_UK_FX, null, DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, "TEST_ORDER_1B", DefaultReference.METAL_FORM_NONE, DefaultReference.METAL_LOCATION_NONE,
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_LIMIT_RELATIVE, DefaultReference.TICKER_XRUCNY,
			TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2005-03-02 08:00:00", TestUser.ANDREW_BAYNES,
			DefaultReference.PRICE_TYPE_SPOT, null,
			"2000-01-15 16:00:00", "2001-04-28", null, 1200.00d,
			DefaultReference.YES_NO_NO, 1150.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE1, 
			DefaultReference.METAL_XPT, DefaultReference.VALIDATION_TYPE_EXPIRY_DATE, null, 1.0d, Arrays.asList(TestFill.TEST_LIMIT_ORDER_FILL_1, TestFill.TEST_LIMIT_ORDER_FILL_2),
			Arrays.asList(TestOrderComment.TEST_COMMENT_1, TestOrderComment.TEST_COMMENT_2)),
	TEST_ORDER_2(100001, 1, TestParty.JM_PMM_US_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_UK_FX, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR,"TEST_ORDER_2", null, null,
			DefaultOrderStatus.LIMIT_ORDER_PENDING, DefaultReference.CONTRACT_TYPE_LIMIT_FIXED, DefaultReference.TICKER_XPTGBP, 
			TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			DefaultReference.PRICE_TYPE_FORWARD, Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_1), 
			"2000-02-15 16:00:00", null, DefaultReference.SYMBOLIC_DATE_1D, 400.00d,
			DefaultReference.YES_NO_YES, 440.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE2, 
			DefaultReference.METAL_XRU, null, "2010-01-31", 1.0d, Arrays.asList(), Arrays.asList()
			),
	TEST_ORDER_3(100002, 1, TestParty.JM_PMM_US_BU, TestParty.JM_PMM_UK_BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, DefaultReference.PORTFOLIO_UK_RUTHENIUM, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR,  "TEST_ORDER_3", DefaultReference.METAL_FORM_GRAIN, DefaultReference.METAL_LOCATION_BRANDENBERGER,
			DefaultOrderStatus.LIMIT_ORDER_FILLED, DefaultReference.CONTRACT_TYPE_LIMIT_FIXED, DefaultReference.TICKER_XAUUSD,
			TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.ARINDAM_RAY,
			DefaultReference.PRICE_TYPE_SPOT_PLUS, Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_4, TestCreditCheck.TEST_CREDIT_CHECK_5), 
			"2000-02-15 16:00:00", null, null, 400.00d,
			DefaultReference.YES_NO_YES, 440.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE2, 
			DefaultReference.METAL_XRU, null, null, 1.0d, null, null
			),
	;
	
	private LimitOrderTo limitOrder;
	
	private TestLimitOrder (long orderId, int version, TestParty internalBu,  TestParty externalBu, 
			DefaultReference intPfolio, DefaultReference extPfolio,
			DefaultReference buySell,
			DefaultReference baseCurrency, Double baseQuantity, DefaultReference baseQuantityUnit, 
			DefaultReference termCurrency, String reference, DefaultReference metalForm, 
			DefaultReference metalLocation,
			DefaultOrderStatus orderStatus, DefaultReference contractType,  
			DefaultReference ticker,
			TestUser createdBy, String createdAt,
			String lastUpdate, TestUser updatedByUser, DefaultReference priceType, 
			List<TestCreditCheck> creditChecks, // << order fields
			String settleDate, String startDateConcrete, DefaultReference startDateSymbolic, double limitPrice, 
			DefaultReference yesNoPartFillable, double spotPrice, DefaultReference stopTriggerType,
			DefaultReference currencyCrossMetal, DefaultReference validationType, String expiryDate, Double executionLikelihood, 
			List<TestFill> fills, List<TestOrderComment> comments) { // << limit order fields
		// order type has to be limit order always
		limitOrder = ImmutableLimitOrderTo.builder()
				.idOrderType(DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity().id())				
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
				.idUpdatedByUser(updatedByUser.getEntity().id()) // << order fields
				.idPriceType(priceType.getEntity().id())
				.settleDate(settleDate)
				.startDateConcrete(startDateConcrete)
				.idStartDateSymbolic(startDateSymbolic != null?startDateSymbolic.getEntity().id():null)
				.limitPrice(limitPrice)
				.idYesNoPartFillable(yesNoPartFillable.getEntity().id())
				.idStopTriggerType(stopTriggerType.getEntity().id())
				.idCurrencyCrossMetal(currencyCrossMetal.getEntity().id())
				.expiryDate(expiryDate)
				.idValidationType(validationType != null?validationType.getEntity().id():null)
				.executionLikelihood(executionLikelihood)
				.fillIds(fills!=null?fills.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.creditChecksIds(creditChecks!=null?creditChecks.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.orderCommentIds(comments!=null?comments.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.fillPercentage(0.0d)
				.idContractType(contractType.getEntity().id())
				.idTicker(ticker.getEntity().id())
				.build();
	}

	public LimitOrderTo getEntity () {
		return limitOrder;
	}

	public void setEntity (LimitOrderTo newLimitOrder) {
		this.limitOrder = newLimitOrder;
	}

	
	public static List<LimitOrderTo> asList () {
		return Arrays.asList(TestLimitOrder.values())
				.stream().map(TestLimitOrder::getEntity).collect(Collectors.toList());
	}	
	
	public static List<TestLimitOrder> asEnumList () {
		return Arrays.asList(TestLimitOrder.values())
				.stream().collect(Collectors.toList());
	}	
}

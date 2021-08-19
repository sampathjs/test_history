package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultExpirationStatus;
import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;

public enum TestLimitOrder {
	TEST_ORDER_1(1000000, TestParty.JM_PMM_UK_BU, TestParty.ANGLO_PLATINUM_BU, 
			null, null, DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, DefaultReference.YES_NO_YES,
			DefaultOrderStatus.LIMIT_ORDER_PENDING, TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2000-01-01 08:00:00", TestUser.ANDREW_BAYNES,
			DefaultReference.PRICE_TYPE_SPOT, null,
			"2000-01-15 16:00:00", DefaultExpirationStatus.LIMIT_ORDER_ACTIVE, 1200.00d,
			DefaultReference.YES_NO_NO, 1150.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE1, 
			DefaultReference.METAL_XPT, 1.0d, Arrays.asList(TestFill.TEST_LIMIT_ORDER_FILL_1, TestFill.TEST_LIMIT_ORDER_FILL_2),
			Arrays.asList(TestOrderComment.TEST_COMMENT_1, TestOrderComment.TEST_COMMENT_2)),
	TEST_ORDER_2(1000001, TestParty.JM_PMM_US_BU, TestParty.ANGLO_PLATINUM_BU, 
			null, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, DefaultReference.YES_NO_NO,
			DefaultOrderStatus.LIMIT_ORDER_PENDING, TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			DefaultReference.PRICE_TYPE_FORWARD, Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_1), 
			"2000-02-15 16:00:00", DefaultExpirationStatus.LIMIT_ORDER_ACTIVE, 400.00d,
			DefaultReference.YES_NO_YES, 440.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE2, 
			DefaultReference.METAL_XRU, 1.0d, Arrays.asList(), Arrays.asList()
			),
	TEST_ORDER_3(1000002, TestParty.JM_PMM_US_BU, TestParty.JM_PMM_UK_BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, DefaultReference.PORTFOLIO_UK_RUTHENIUM, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, DefaultReference.YES_NO_NO,
			DefaultOrderStatus.LIMIT_ORDER_FILLED, TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.ARINDAM_RAY,
			DefaultReference.PRICE_TYPE_SPOT_PLUS, Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_4, TestCreditCheck.TEST_CREDIT_CHECK_5), 
			"2000-02-15 16:00:00", DefaultExpirationStatus.LIMIT_ORDER_ACTIVE, 400.00d,
			DefaultReference.YES_NO_YES, 440.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE2, 
			DefaultReference.METAL_XRU, 1.0d, null, null
			),
	;
	
	private LimitOrderTo limitOrder;
	
	private TestLimitOrder (long orderId, TestParty internalBu,  TestParty externalBu, 
			DefaultReference intPfolio, DefaultReference extPfolio,
			DefaultReference buySell,
			DefaultReference baseCurrency, Double baseQuantity, DefaultReference baseQuantityUnit, 
			DefaultReference termCurrency, DefaultReference yesNoPhysicalDeliveryRequired,
			DefaultOrderStatus orderStatus, TestUser createdBy, String createdAt,
			String lastUpdate, TestUser updatedByUser, DefaultReference priceType, 
			List<TestCreditCheck> creditChecks, // << order fields
			String settleDate, DefaultExpirationStatus expirationStatus, double price, 
			DefaultReference yesNoPartFillable, double spotPrice, DefaultReference stopTriggerType,
			DefaultReference currencyCrossMetal, Double executionLikelihood, 
			List<TestFill> fills, List<TestOrderComment> comments) { // << limit order fields
		// order type has to be limit order always
		limitOrder = ImmutableLimitOrderTo.builder()
				.id(orderId)
				.version(0)
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
				.idYesNoPhysicalDeliveryRequired(yesNoPhysicalDeliveryRequired.getEntity().id())
				.idOrderStatus(orderStatus.getEntity().id())
				.createdAt(createdAt)
				.idCreatedByUser(createdBy.getEntity().id())
				.lastUpdate(lastUpdate)
				.idUpdatedByUser(updatedByUser.getEntity().id()) // << order fields
				.idPriceType(priceType.getEntity().id())
				.settleDate(settleDate)
				.idExpirationStatus(expirationStatus.getEntity().id())
				.price(price)
				.idYesNoPartFillable(yesNoPartFillable.getEntity().id())
				.spotPrice(spotPrice)
				.idStopTriggerType(stopTriggerType.getEntity().id())
				.idCurrencyCrossMetal(currencyCrossMetal.getEntity().id())
				.executionLikelihood(executionLikelihood)
				.fillIds(fills!=null?fills.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.creditChecksIds(creditChecks!=null?creditChecks.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.orderCommentIds(comments!=null?comments.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
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

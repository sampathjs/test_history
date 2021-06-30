package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

public enum TestReferenceOrder {
	TEST_ORDER_1(1001, TestParty.JM_PMM_UK_BU, TestParty.ANGLO_PLATINUM_BU, 
			null, null,
			DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, DefaultReference.PAYMENT_PERIOD_SAMPLE1, DefaultReference.YES_NO_YES,
			DefaultOrderStatus.LIMIT_ORDER_PENDING, TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2000-01-01 08:00:00", TestUser.ANDREW_BAYNES,
			DefaultReference.PRICE_TYPE_SAMPLE2,  Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_1),
			TestIndex.INDEX_PX_XPT_GBP, TestIndex.INDEX_FX_EUR_CHF, 
			"2000-02-01 08:00:00", "2000-04-01 08:00:00", DefaultReference.AVERAGING_RULES_SAMPLE2,
			Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_1)
			),
	TEST_ORDER_2(1002, TestParty.JM_PMM_US_BU, TestParty.ANGLO_PLATINUM_BU, 
			null, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, DefaultReference.PAYMENT_PERIOD_SAMPLE2, DefaultReference.YES_NO_NO,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			DefaultReference.PRICE_TYPE_SAMPLE2, 
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_2, TestCreditCheck.TEST_CREDIT_CHECK_6), TestIndex.INDEX_PX_XAG_USD, TestIndex.INDEX_FX_EUR_CHF, 
			"2000-02-15 16:00:00", "2000-04-15 16:00:00", DefaultReference.AVERAGING_RULES_SAMPLE1,
			null
			),		
	TEST_ORDER_3(1003, TestParty.JM_PMM_US_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, DefaultReference.PAYMENT_PERIOD_SAMPLE2, DefaultReference.YES_NO_NO,
			DefaultOrderStatus.REFERENCE_ORDER_FILLED, TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			DefaultReference.PRICE_TYPE_SAMPLE2, 
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_7), TestIndex.INDEX_PX_XAG_USD, TestIndex.INDEX_FX_EUR_CHF, 
			"2000-02-15 16:00:00", "2000-04-15 16:00:00", DefaultReference.AVERAGING_RULES_SAMPLE1,
			null
			),
	;
	
	private ReferenceOrderTo referenceOrder;
	
	private TestReferenceOrder (int orderId, TestParty internalBu, TestParty externalBu,
			DefaultReference intPfolio, DefaultReference extPfolio,
			DefaultReference buySell,
			DefaultReference baseCurrency, Double baseQuantity, DefaultReference baseQuantityUnit, 
			DefaultReference termCurrency, DefaultReference paymentPeriod, DefaultReference yesNoPhysicalDeliveryRequired,
			DefaultOrderStatus orderStatus, TestUser createdBy, String createdAt,
			String lastUpdate, TestUser updatedByUser, DefaultReference priceType, 
			List<TestCreditCheck> creditChecks,
			// << order fields
		    TestIndex metalReferenceIndex, TestIndex currencyReferenceIndex, 
		    String fixingStartDate, String fixingEndDate, DefaultReference averagingRule, 
			List<TestFill> fills  // << reference order fields
			) {
		// order type has to be limit order always
		referenceOrder = ImmutableReferenceOrderTo.builder()
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
				.idPaymentPeriod(paymentPeriod.getEntity().id())
				.idYesNoPhysicalDeliveryRequired(yesNoPhysicalDeliveryRequired.getEntity().id())
				.idOrderStatus(orderStatus.getEntity().id())
				.createdAt(createdAt)
				.idCreatedByUser(createdBy.getEntity().id())
				.lastUpdate(lastUpdate)
				.idUpdatedByUser(updatedByUser.getEntity().id()) // << order fields
				.idPriceType(priceType.getEntity().id())
				.idMetalReferenceIndex(metalReferenceIndex.getEntity().id())
				.idCurrencyReferenceIndex(currencyReferenceIndex.getEntity().id())
				.fixingStartDate(fixingStartDate)
				.fixingEndDate(fixingEndDate)
				.idAveragingRule(averagingRule.getEntity().id())
				.fillIds (fills!=null?fills.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.creditChecksIds((creditChecks!=null?creditChecks.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
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
	
	public static List<TestReferenceOrder> asEnumList () {
		return Arrays.asList(TestReferenceOrder.values())
				.stream().collect(Collectors.toList());
	}	
}

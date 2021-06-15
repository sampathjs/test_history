package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.*;


import com.matthey.pmm.toms.model.ImmutableLimitOrder;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum TestLimitOrder {
	TEST_ORDER_1(1, DefaultParty.JM_PMM_UK_BU, DefaultParty.ANGLO_PLATINUM_BU, DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, DefaultReference.PAYMENT_PERIOD_SAMPLE1, DefaultReference.YES_NO_YES,
			DefaultOrderStatus.LIMIT_ORDER_NEW, DefaultUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2000-01-01 08:00:00", DefaultUser.ANDREW_BAYNES,
			DefaultReference.PRICE_TYPE_SAMPLE1, "2000-01-15 16:00:00", DefaultExpirationStatus.LIMIT_ORDER_ACTIVE, 1200.00d,
			DefaultReference.YES_NO_NO, 1150.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE1, 
			DefaultReference.METAL_XPT, 1.0d
			),
	TEST_ORDER_2(2, DefaultParty.JM_PMM_US_BU, DefaultParty.ANGLO_PLATINUM_BU, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, DefaultReference.PAYMENT_PERIOD_SAMPLE2, DefaultReference.YES_NO_NO,
			DefaultOrderStatus.LIMIT_ORDER_WAITING_APPROVAL, DefaultUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", DefaultUser.PAT_MCCOURT,
			DefaultReference.PRICE_TYPE_SAMPLE2, "2000-02-15 16:00:00", DefaultExpirationStatus.LIMIT_ORDER_ACTIVE, 400.00d,
			DefaultReference.YES_NO_YES, 440.00d, DefaultReference.STOP_TRIGGER_TYPE_SAMPLE2, 
			DefaultReference.METAL_XRU, 1.0d
			),	
	;
	
	private final LimitOrder limitOrder;
	
	private TestLimitOrder (int orderId, DefaultParty internalParty, DefaultParty counterParty, DefaultReference buySell,
			DefaultReference metalCurrency, double quantity, DefaultReference quantityUnit, 
			DefaultReference currency, DefaultReference paymentPeriod, DefaultReference yesNoPhysicalDeliveryRequired,
			DefaultOrderStatus orderStatus, DefaultUser createdBy, String createdAt,
			String lastUpdate, DefaultUser updatedByUser, // << order fields
			DefaultReference priceType, String settleDate, DefaultExpirationStatus expirationStatus, double price, 
			DefaultReference yesNoPartFillable, double spotPrice, DefaultReference stopTriggerType,
			DefaultReference currencyCrossMetal, Double executionLikelihood // << limit order fields
			) {
		// order type has to be limit order always
		limitOrder = ImmutableLimitOrder.builder()
				.id(orderId)
				.idInternalParty(internalParty.getEntity().id())
				.idExternalParty(counterParty.getEntity().id())
				.idBuySell (buySell.getEntity().id())
				.idMetalCurrency(metalCurrency.getEntity().id())
				.quantity(quantity)
				.idQuantityUnit(quantityUnit.getEntity().id())
				.idCurrency(currency.getEntity().id())
				.idOrderType(DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity().id())
				.idPaymentPeriod(paymentPeriod.getEntity().id())
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
				.build();
	}

	public LimitOrder getEntity () {
		return limitOrder;
	}
	
	public static List<LimitOrder> asList () {
		return Arrays.asList(TestLimitOrder.values())
				.stream().map(TestLimitOrder::getEntity).collect(Collectors.toList());
	}	
}

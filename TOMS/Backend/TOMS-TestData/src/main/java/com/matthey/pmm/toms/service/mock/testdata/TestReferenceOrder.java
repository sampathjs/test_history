package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

public enum TestReferenceOrder {
	TEST_ORDER_1A(100003, 1, TestParty.JM_PMM_UK_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_UK_PLATINUM, null,
			DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, "TEST_ORDER_1A", DefaultReference.METAL_FORM_INGOT, DefaultReference.METAL_LOCATION_ROYSTON,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2000-01-01 08:00:00", TestUser.ANDREW_BAYNES,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_8),
			// reference order fields
			DefaultReference.CONTRACT_TYPE_AVERAGE, 100d, 
			30d, 45d, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_1),
			null, null
			),
	TEST_ORDER_1B(100003, 2, TestParty.JM_PMM_UK_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_UK_PLATINUM, null,
			DefaultReference.BUY_SELL_BUY,
			DefaultReference.METAL_XPT, 1000d, DefaultReference.QUANTITY_TOZ, 
			DefaultReference.CCY_GBP, "TEST_ORDER_1B", DefaultReference.METAL_FORM_NONE, DefaultReference.METAL_LOCATION_NONE,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, TestUser.ANDREW_BAYNES, "2000-01-01 08:00:00", "2005-12-23 12:00:00", TestUser.ANDREW_BAYNES,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_8),
			// reference order fields
			DefaultReference.CONTRACT_TYPE_AVERAGE, null, 
			30d, 45d, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_1, TestReferenceOrderLeg.TEST_LEG_2),
			Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_1), Arrays.asList(TestOrderComment.TEST_COMMENT_4, TestOrderComment.TEST_COMMENT_5)
			),
	TEST_ORDER_2(100004, 1, TestParty.JM_PMM_US_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_ORDER_2", null, null,
			DefaultOrderStatus.REFERENCE_ORDER_PENDING, TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_2, TestCreditCheck.TEST_CREDIT_CHECK_6),  
			// reference order fields
			DefaultReference.CONTRACT_TYPE_FIXING, 100d, 
			null, 45d, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_9),
			Arrays.asList(TestFill.TEST_REFERENCE_ORDER_FILL_1), Arrays.asList(TestOrderComment.TEST_COMMENT_6)
			),		
	TEST_ORDER_3(100005, 1, TestParty.JM_PMM_US_BU, TestParty.ANGLO_PLATINUM_BU, 
			DefaultReference.PORTFOLIO_US_RUTHENIUM, null, DefaultReference.BUY_SELL_SELL,
			DefaultReference.METAL_XRU, 1d, DefaultReference.QUANTITY_MT, 
			DefaultReference.CCY_EUR, "TEST_ORDER_3", DefaultReference.METAL_FORM_GRAIN, DefaultReference.METAL_LOCATION_BRANDENBERGER,
			DefaultOrderStatus.REFERENCE_ORDER_FILLED, TestUser.PAT_MCCOURT, "2000-01-02 16:00:00", "2000-01-02 16:00:00", TestUser.PAT_MCCOURT,
			Arrays.asList(TestCreditCheck.TEST_CREDIT_CHECK_7), 
			// reference order fields
			DefaultReference.CONTRACT_TYPE_FIXING, 100d, 
			30d, null, 
			Arrays.asList(TestReferenceOrderLeg.TEST_LEG_3, TestReferenceOrderLeg.TEST_LEG_4, 
					TestReferenceOrderLeg.TEST_LEG_5, TestReferenceOrderLeg.TEST_LEG_6,
					TestReferenceOrderLeg.TEST_LEG_7, TestReferenceOrderLeg.TEST_LEG_8),
			null, null
			),
	;
	
	private ReferenceOrderTo referenceOrder;
	
	private TestReferenceOrder (long orderId, int version, TestParty internalBu, TestParty externalBu,
			DefaultReference intPfolio, DefaultReference extPfolio,
			DefaultReference buySell,
			DefaultReference baseCurrency, Double baseQuantity, DefaultReference baseQuantityUnit, 
			DefaultReference termCurrency, String reference, DefaultReference metalForm, 
			DefaultReference metalLocation,			
			DefaultOrderStatus orderStatus, TestUser createdBy, String createdAt,
			String lastUpdate, TestUser updatedByUser,  
			List<TestCreditCheck> creditChecks,
			// << order fields
			DefaultReference contractType, Double contangoBackwardation, 
		    Double fxRateSpread, Double metalPriceSpread,
		    List<TestReferenceOrderLeg> legs,
			List<TestFill> fills, List<TestOrderComment> orderComments// << reference order fields
			) {
		// order type has to be limit order always
		referenceOrder = ImmutableReferenceOrderTo.builder()
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
				.fillIds (fills!=null?fills.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null)
				.creditChecksIds((creditChecks!=null?creditChecks.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
				.orderCommentIds((orderComments!=null?orderComments.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
				 // << order fields
				.idContractType(contractType.getEntity().id())
				.contangoBackwardation(contangoBackwardation)
				.fxRateSpread(fxRateSpread)
				.metalPriceSpread(metalPriceSpread)
				.addAllLegIds((legs!=null?legs.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()):null))
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

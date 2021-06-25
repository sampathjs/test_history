package com.matthey.pmm.toms.service.mock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.matthey.pmm.toms.enums.DefaultExpirationStatus;
import com.matthey.pmm.toms.enums.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.DefaultProcessTransition;
import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.exception.IllegalValueException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.testdata.TestIndex;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.UserTo;

public class SharedMockLogic {
	public static LimitOrderTo validateLimitOrderId(Class clazz, String method, String argument,
			int limitOrderId, Stream<LimitOrderTo> allDataSources) {
		List<LimitOrderTo> limitOrders = allDataSources
    		.filter(x -> x.id() == limitOrderId)
    		.collect(Collectors.toList());
    	
    	if (limitOrders.size() == 0) {
    		throw new IllegalIdException(clazz, method, argument, "<unknown>", "" + limitOrderId);
    	}
    	LimitOrderTo limitOrder = limitOrders.get(0);
    	return limitOrder;
	}
	
	public static ReferenceOrderTo validateReferenceOrderId(Class clazz, String method, String argument,
			int referenceOrderId, Stream<ReferenceOrderTo> allDataSources) {
		List<ReferenceOrderTo> referenceOrders = allDataSources
    		.filter(x -> x.id() == referenceOrderId)
    		.collect(Collectors.toList());
    	
    	if (referenceOrders.size() == 0) {
    		throw new IllegalIdException(clazz, method, argument, "<unknown>", "" + referenceOrderId);
    	}
    	ReferenceOrderTo referenceOrder = referenceOrders.get(0);
    	return referenceOrder;
	}
	
	public static void validateLimitOrderFields (Class clazz, String method, String argument, LimitOrderTo order, boolean isNew, LimitOrderTo oldLimitOrder) {
		validateOrderFields (clazz, method, argument, order, isNew, oldLimitOrder);
		// validate input data
		if (order.idOrderType() != DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity().id()) {
			throw new IllegalValueException(clazz, method + ".idOrderType", argument , " = " + DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity().id(), "" + order.idOrderType());
		}		
		
		SimpleDateFormat sdfDate = new SimpleDateFormat (TomsService.DATE_FORMAT);
		try {
			Date parsedTime = sdfDate.parse (order.settleDate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".settleDate", TomsService.DATE_FORMAT, order.settleDate());
		}
    	if (!DefaultExpirationStatus.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idExpirationStatus()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idExpirationStatus" , "Expiration Status", "" + order.idExpirationStatus());
    	}
		
    	if (order.price() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".price", " > 0", "" + order.price());
    	}

    	TomsService.verifyDefaultReference (order.idYesNoPartFillable(),
				Arrays.asList(DefaultReferenceType.YES_NO),
				MockOrderController.class, method , argument + ".idYesNoPartFillable");

    	if (order.spotPrice() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".spotPrice", " > 0", "" + order.spotPrice());
    	}
    	
    	TomsService.verifyDefaultReference (order.idStopTriggerType(),
				Arrays.asList(DefaultReferenceType.STOP_TRIGGER_TYPE),
				MockOrderController.class, method , argument + ".idStopTriggerType");
    	
    	TomsService.verifyDefaultReference (order.idCurrencyCrossMetal(),
				Arrays.asList(DefaultReferenceType.CCY_METAL),
				MockOrderController.class, method , argument + ".idCurrencyCrossMetal");

    	if (order.executionLikelihood() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".executionLikelihood", " > 0", "" + order.executionLikelihood());
    	}
    	
    	if (!isNew) {
        	// verify status change
    		List<ProcessTransitionTo> availableTransitions = DefaultProcessTransition.asList().stream().filter(
    				x -> x.referenceCategoryId() == DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id()
    			&&       x.fromStatusId() == oldLimitOrder.idOrderStatus()
    			&&       x.toStatusId() == order.idOrderStatus())
    				.collect(Collectors.toList());
    		if (availableTransitions.size() == 0) {
    			OrderStatusTo fromStatus = DefaultOrderStatus.findById (oldLimitOrder.idOrderStatus()).get();
    			ReferenceTo fromStatusName = DefaultReference.findById (fromStatus.idOrderStatusName()).get();    			
    			OrderStatusTo toStatus = DefaultOrderStatus.findById (order.idOrderStatus()).get();
    			ReferenceTo toStatusName = DefaultReference.findById (toStatus.idOrderStatusName()).get();
        		List<String> possibleTransitions = DefaultProcessTransition.asList().stream().filter(
        				x -> x.referenceCategoryId() == DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id()
        			&&       x.fromStatusId() == oldLimitOrder.idOrderStatus())
        				.map(x -> DefaultReference.findById(DefaultOrderStatus.findById(x.fromStatusId()).get().id()).get().name() + " -> " + 
        						DefaultReference.findById(DefaultOrderStatus.findById(x.toStatusId()).get().id()).get().name())
        				.collect(Collectors.toList());
    			
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.name(), toStatusName.name(), possibleTransitions.toString());
    		}
    	}
	}
	
	public static void validateReferenceOrderFields (Class clazz, String method, String argument, ReferenceOrderTo order, boolean isNew, ReferenceOrderTo oldReferenceOrder) {
		validateOrderFields (clazz, method, argument, order, isNew, oldReferenceOrder);
	    
    	if (!TestIndex.asListMetal().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idMetalReferenceIndex()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idMetalReferenceIndex", "Index (metal)", "" + order.idMetalReferenceIndex());
    	}
    	
    	if (!TestIndex.asListCurrency().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idCurrencyReferenceIndex()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCurrencyReferenceIndex", "Index (pure currency)", "" + order.idCurrencyReferenceIndex());
    	}

		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (order.fixingStartDate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".fixingStartDate", TomsService.DATE_TIME_FORMAT, order.lastUpdate());
		}

		try {
			Date parsedTime = sdfDateTime.parse (order.fixingEndDate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".fixingEndDate", TomsService.DATE_TIME_FORMAT, order.lastUpdate());
		}

    	TomsService.verifyDefaultReference (order.idAveragingRule(),
				Arrays.asList(DefaultReferenceType.AVERAGING_RULE),
				MockOrderController.class, method , argument + ".idAveragingRule");
	}

	
	private static void validateOrderFields (Class clazz, String method, String argument, OrderTo order, boolean newOrder, OrderTo oldOrder) {
    	if (newOrder) {
    		if (order.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + order.id());
        	}    		
    	} else {
    		if (order.id() != oldOrder.id()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldOrder.id(), "" + order.id());
        	}    		
    	}
    	
    	if (!TestParty.asListInternal().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idInternalParty()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idInternalParty", "Party (internal)", "" + order.idInternalParty());
    	}
    	
    	if (!TestParty.asListExternal().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idExternalParty()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idExternalParty", "Party (external)", "" + order.idExternalParty());
    	}
    	
    	TomsService.verifyDefaultReference (order.idBuySell(),
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				MockOrderController.class, method , argument + ".idBuySell");    
    	
    	TomsService.verifyDefaultReference (order.idMetalCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_METAL),
				MockOrderController.class, method , argument + ".idMetalCurrency");
    	
    	if (order.quantity() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".quantity", " > 0", "" + order.quantity());
    	}

    	TomsService.verifyDefaultReference (order.idQuantityUnit(),
				Arrays.asList(DefaultReferenceType.QUANTITY_UNIT),
				MockOrderController.class, method , argument + ".idQuantityUnit");
    	
    	TomsService.verifyDefaultReference (order.idCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY),
				MockOrderController.class, method , argument + ".idCurrency");

    	TomsService.verifyDefaultReference (order.idPaymentPeriod(),
				Arrays.asList(DefaultReferenceType.PAYMENT_PERIOD),
				MockOrderController.class, method , argument + ".idPaymentPeriod");

    	TomsService.verifyDefaultReference (order.idYesNoPhysicalDeliveryRequired(),
				Arrays.asList(DefaultReferenceType.YES_NO),
				MockOrderController.class, method , argument + ".idYesNoPhysicalDeliveryRequired");

    	if (!DefaultOrderStatus.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idOrderStatus()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idOrderStatus" , "Order Status", "" + order.idOrderStatus());
    	}
    	
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (order.createdAt());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".createdAt", TomsService.DATE_TIME_FORMAT, order.createdAt());
		}
		
    	if (!TestUser.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idCreatedByUser()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCreatedByUser" , "User", "" + order.idCreatedByUser());
    	}    	
    	UserTo createdBy = TestUser.asList().stream().filter(x -> x.id() == order.idCreatedByUser()).collect(Collectors.toList()).get(0);
    	if (!createdBy.tradeableInternalPartyIds().contains (order.idInternalParty())) {
    		throw new IllegalValueException (clazz, method, argument + ".idInternalParty", 
    				"Not matching allowed internal parties for provided createdBy " + createdBy.id() + " :" + createdBy.tradeableInternalPartyIds(), "" + order.idInternalParty());
    	}
    	if (!createdBy.tradeableCounterPartyIds().contains (order.idExternalParty())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExternalParty", 
    				"Not matching allowed external parties for provided createdBy " + createdBy.id() + " :" + createdBy.tradeableCounterPartyIds(), "" + order.idExternalParty());
    	}

		try {
			Date parsedTime = sdfDateTime.parse (order.lastUpdate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdate", TomsService.DATE_TIME_FORMAT, order.lastUpdate());
		}

    	if (!TestUser.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idUpdatedByUser()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedByUser" , "User", "" + order.idUpdatedByUser());
    	}    	
    	UserTo updatedBy = TestUser.asList().stream().filter(x -> x.id() == order.idUpdatedByUser()).collect(Collectors.toList()).get(0);
    	if (!createdBy.tradeableInternalPartyIds().contains (order.idInternalParty())) {
    		throw new IllegalValueException (clazz, method, argument + ".idInternalParty", 
    				"Not matching allowed internal parties for provided updatedBy " + updatedBy.id() + " :" + updatedBy.tradeableInternalPartyIds(), "" + order.idInternalParty());
    	}
    	if (!createdBy.tradeableCounterPartyIds().contains (order.idExternalParty())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExternalParty", 
    				"Not matching allowed external parties for provided updatedBy " + updatedBy.id() + " :" + updatedBy.tradeableCounterPartyIds(), "" + order.idExternalParty());
    	}
    	
    	TomsService.verifyDefaultReference (order.idPriceType(),
				Arrays.asList(DefaultReferenceType.PRICE_TYPE),
				MockOrderController.class, method , argument + ".idPriceType");
	}
	
	public static void validateFillFields (Class clazz, String method, String argument, FillTo orderFill, boolean isNew, FillTo oldOrderFillTo) {
    	if (isNew) {
    		if (orderFill.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + orderFill.id());
        	}
    	} 
    	
    	if (orderFill.fillQuantity() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fillQuantity", " > 0", "" + orderFill.fillQuantity());
    	}

    	if (orderFill.fillPrice() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fillPrice", " > 0", "" + orderFill.fillPrice());
    	}
    	// can't validate Endur side ID (idTrade) 
    	
    	if (!TestUser.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( orderFill.idTrader()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idTrader" , "User", "" + orderFill.idTrader());
    	}

    	if (!TestUser.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( orderFill.idUpdatedBy()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedBy" , "User", "" + orderFill.idUpdatedBy());
    	}
    	
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (orderFill.lastUpdateDateTime());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdateDateTime", TomsService.DATE_TIME_FORMAT, orderFill.lastUpdateDateTime());
		}    	
	}
	
	public static void validateCreditCheckFields (Class clazz, String method, String argument, CreditCheckTo creditCheck, boolean isNew, CreditCheckTo oldCreditCheck) {
    	if (isNew) {
    		if (creditCheck.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + creditCheck.id());
        	}
    	} else {
    		if (creditCheck.id() != oldCreditCheck.id()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldCreditCheck.id(), "" + creditCheck.id());
        	}
    	}
    	
    	if (!TestParty.asListExternal().stream().map(x -> x.id()).collect(Collectors.toList()).contains( creditCheck.idParty()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idParty", "Party (External)", "" + creditCheck.idParty());
    	}

    	if (creditCheck.creditLimit() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".fillQuantity", " > 0", "" + creditCheck.creditLimit());
    	}
    	if (creditCheck.currentUtilization() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".currentUtilization", " > 0", "" + creditCheck.currentUtilization());
    	}

		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (creditCheck.runDateTime());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".runDateTime", TomsService.DATE_TIME_FORMAT, creditCheck.runDateTime());
		}
		
    	TomsService.verifyDefaultReference (creditCheck.idCreditCheckRunStatus(),
				Arrays.asList(DefaultReferenceType.CREDIT_CHECK_RUN_STATUS),
				MockOrderController.class, method , argument + ".idCreditCheckRunStatus");

    	TomsService.verifyDefaultReference (creditCheck.idCreditCheckOutcome(),
				Arrays.asList(DefaultReferenceType.CREDIT_CHECK_OUTCOME),
				MockOrderController.class, method , argument + ".idCreditCheckOutcome");
    	
    	if (!isNew) {
        	// verify status change
    		List<ProcessTransitionTo> availableTransitions = DefaultProcessTransition.asList().stream().filter(
    				x -> x.referenceCategoryId() == DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION.getEntity().id()
    			&&       x.fromStatusId() == oldCreditCheck.idCreditCheckRunStatus()
    			&&       x.toStatusId() == creditCheck.idCreditCheckRunStatus())
    				.collect(Collectors.toList());
    		if (availableTransitions.size() == 0) {
    			ReferenceTo fromStatusName = DefaultReference.findById (oldCreditCheck.idCreditCheckRunStatus()).get();    			
    			ReferenceTo toStatusName = DefaultReference.findById (creditCheck.idCreditCheckRunStatus()).get();
        		List<String> possibleTransitions = DefaultProcessTransition.asList().stream().filter(
        				x -> x.referenceCategoryId() == DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION.getEntity().id()
        			&&       x.fromStatusId() == oldCreditCheck.idCreditCheckRunStatus())
        				.map(x -> DefaultReference.findById(DefaultOrderStatus.findById(x.fromStatusId()).get().id()).get().name() + " -> " + 
        						DefaultReference.findById(DefaultOrderStatus.findById(x.toStatusId()).get().id()).get().name())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.name(), toStatusName.name(), possibleTransitions.toString());
    		}
    	}
	}
}

package com.matthey.pmm.toms.service.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.matthey.pmm.toms.enums.v1.DefaultExpirationStatus;
import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultProcessTransition;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.exception.IllegalValueException;
import com.matthey.pmm.toms.service.exception.IllegalVersionException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.testdata.TestIndex;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.UserTo;

public class SharedMockLogic {
	private static final double EPSILON = 0.00001d; 
	
	public static <T extends OrderTo> T validateOrderId(Class serviceClass, String method, String argument,
			long orderId, Stream<OrderTo> allDataSources, Class<T> orderClass) {
		List<OrderTo> limitOrders = allDataSources
    		.filter(x -> x.id() == orderId && orderClass.isInstance(x))
    		.collect(Collectors.toList());
    	
    	if (limitOrders.size() == 0) {
    		throw new IllegalIdException(serviceClass, method, argument, "<unknown>", "" + orderId);
    	}
    	OrderTo limitOrder = limitOrders.get(0);
    	return (T)limitOrder;
	}
	
	public static ReferenceOrderTo validateReferenceOrderId(Class clazz, String method, String argument,
			long referenceOrderId, Stream<OrderTo> allDataSources) {
		List<OrderTo> referenceOrders = allDataSources
    		.filter(x -> x.id() == referenceOrderId && x instanceof ReferenceOrderTo)
    		.collect(Collectors.toList());
    	
    	if (referenceOrders.size() == 0) {
    		throw new IllegalIdException(clazz, method, argument, "<unknown>", "" + referenceOrderId);
    	}
    	ReferenceOrderTo referenceOrder = (ReferenceOrderTo)referenceOrders.get(0);
    	return referenceOrder;
	}
	
	public static void validateLimitOrderFields (Class clazz, String method, String argument, LimitOrderTo order, boolean isNew, OrderTo oldLimitOrder) {
		validateOrderFields (clazz, method, argument, order, isNew, oldLimitOrder);
		// validate input data		
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
    	
    	TomsService.verifyDefaultReference (order.idPriceType(),
				Arrays.asList(DefaultReferenceType.PRICE_TYPE),
				MockOrderController.class, method , argument + ".idPriceType", false);


    	TomsService.verifyDefaultReference (order.idYesNoPartFillable(),
				Arrays.asList(DefaultReferenceType.YES_NO),
				MockOrderController.class, method , argument + ".idYesNoPartFillable", false);

    	if (order.spotPrice() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".spotPrice", " > 0", "" + order.spotPrice());
    	}
    	
    	TomsService.verifyDefaultReference (order.idStopTriggerType(),
				Arrays.asList(DefaultReferenceType.STOP_TRIGGER_TYPE),
				MockOrderController.class, method , argument + ".idStopTriggerType", false);
    	
    	TomsService.verifyDefaultReference (order.idCurrencyCrossMetal(),
				Arrays.asList(DefaultReferenceType.CCY_METAL),
				MockOrderController.class, method , argument + ".idCurrencyCrossMetal", false);

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
    		} else {
    			verifyUnchangedStates (clazz, method, argument, availableTransitions.get(0), oldLimitOrder, order);    			
    		}
    	}
	}
	
	public static void validateReferenceOrderFields (Class clazz, String method, String argument, ReferenceOrderTo order, boolean isNew, OrderTo oldReferenceOrder) {
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
				MockOrderController.class, method , argument + ".idAveragingRule", false);
    	
    	if (!isNew) {
        	// verify status change
    		List<ProcessTransitionTo> availableTransitions = DefaultProcessTransition.asList().stream().filter(
    				x -> x.referenceCategoryId() == DefaultReference.REFERENCE_ORDER_TRANSITION.getEntity().id()
    			&&       x.fromStatusId() == oldReferenceOrder.idOrderStatus()
    			&&       x.toStatusId() == order.idOrderStatus())
    				.collect(Collectors.toList());
    		if (availableTransitions.size() == 0) {
    			OrderStatusTo fromStatus = DefaultOrderStatus.findById (oldReferenceOrder.idOrderStatus()).get();
    			ReferenceTo fromStatusName = DefaultReference.findById (fromStatus.idOrderStatusName()).get();    			
    			OrderStatusTo toStatus = DefaultOrderStatus.findById (order.idOrderStatus()).get();
    			ReferenceTo toStatusName = DefaultReference.findById (toStatus.idOrderStatusName()).get();
        		List<String> possibleTransitions = DefaultProcessTransition.asList().stream().filter(
        				x -> x.referenceCategoryId() == DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id()
        			&&       x.fromStatusId() == oldReferenceOrder.idOrderStatus())
        				.map(x -> DefaultReference.findById(DefaultOrderStatus.findById(x.fromStatusId()).get().id()).get().name() + " -> " + 
        						DefaultReference.findById(DefaultOrderStatus.findById(x.toStatusId()).get().id()).get().name())
        				.collect(Collectors.toList());
    			throw new IllegalStateChangeException (clazz, method,
    					argument, fromStatusName.name(), toStatusName.name(), possibleTransitions.toString());
    		} else {
    			verifyUnchangedStates (clazz, method, argument, availableTransitions.get(0), oldReferenceOrder, order);
    		}
    	}
	}
	
	private static void validateOrderFields (Class clazz, String method, String argument, OrderTo order, boolean newOrder, OrderTo oldOrder) {
    	if (newOrder) {
    		if (order.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + order.id());
        	} 
    		if (order.version() != 0) {
        		throw new IllegalVersionException(clazz, method, argument  + ".version", "0", "" + order.version());    			
    		}
    	} else {
    		if (order.id() != oldOrder.id()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldOrder.id(), "" + order.id());
        	}
        	if (order.version() != oldOrder.version()) {
        		throw new IllegalVersionException(clazz, method, argument  + ".version", "" + oldOrder.version(), "" + order.version());
        	}
    	}
    	
    	if (!TestParty.asListInternal().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idInternalBu()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idInternalBu", "Party (internal)", "" + order.idInternalBu());
    	}
    	
    	if (!TestParty.asListExternal().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idExternalBu()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idExternalBu", "Party (external)", "" + order.idExternalBu());
    	}
    	
    	// because of the check above we know that the following optional is always going to contain a value.
    	Optional <PartyTo> internalBu = TestParty.findById(order.idInternalBu());
    	if (order.idInternalLe() != internalBu.get().idLegalEntity()) {
    		throw new IllegalIdException (clazz, method, argument + ".idExternalLe", 
    				"LE of provided internal BU: Party ID #" + internalBu.get().idLegalEntity(),
    				"" + order.idInternalLe());
    	}
    	
    	Optional <PartyTo> externalBu = TestParty.findById(order.idExternalBu());
    	if (order.idExternalLe() != externalBu.get().idLegalEntity()) {
    		throw new IllegalIdException (clazz, method, argument + ".idExternalLe", 
    				"LE of provided external BU: Party ID #" + externalBu.get().idLegalEntity(),
    				"" + order.idExternalLe());
    	}
    	   	    	
    	TomsService.verifyDefaultReference (order.idBuySell(),
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				MockOrderController.class, method , argument + ".idBuySell", false);    
    	
    	TomsService.verifyDefaultReference (order.idBaseCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_METAL, DefaultReferenceType.CCY_CURRENCY),
				MockOrderController.class, method , argument + ".idMetalCurrency", false);
    	
    	if (order.baseQuantity() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".quantity", " > 0", "" + order.baseQuantity());
    	}

    	TomsService.verifyDefaultReference (order.idBaseQuantityUnit(),
				Arrays.asList(DefaultReferenceType.QUANTITY_UNIT),
				MockOrderController.class, method , argument + ".idQuantityUnit", false);
    	
    	TomsService.verifyDefaultReference (order.idTermCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY),
				MockOrderController.class, method , argument + ".idCurrency", false);

    	TomsService.verifyDefaultReference (order.idYesNoPhysicalDeliveryRequired(),
				Arrays.asList(DefaultReferenceType.YES_NO),
				MockOrderController.class, method , argument + ".idYesNoPhysicalDeliveryRequired", false);

    	if (!DefaultOrderStatus.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( order.idOrderStatus()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idOrderStatus" , "Order Status", "" + order.idOrderStatus());
    	}
    	
    	TomsService.verifyDefaultReference (order.idExtPortfolio(),
				Arrays.asList(DefaultReferenceType.PORTFOLIO),
				MockOrderController.class, method , argument + ".idExtPortfolio", true);
    	
    	TomsService.verifyDefaultReference (order.idIntPortfolio(),
				Arrays.asList(DefaultReferenceType.PORTFOLIO),
				MockOrderController.class, method , argument + ".idIntPortfolio", true);
    	
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
    	if (!createdBy.tradeableInternalPartyIds().contains (order.idInternalBu())) {
    		throw new IllegalValueException (clazz, method, argument + ".idInternalParty", 
    				"Not matching allowed internal parties for provided createdBy " + createdBy.id() + " :" + createdBy.tradeableInternalPartyIds(), "" + order.idInternalBu());
    	}
    	if (!createdBy.tradeableCounterPartyIds().contains (order.idExternalBu())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExternalParty", 
    				"Not matching allowed external parties for provided createdBy " + createdBy.id() + " :" + createdBy.tradeableCounterPartyIds(), "" + order.idExternalBu());
    	}

    	if (order.idIntPortfolio() != null && !createdBy.tradeablePortfolioIds().contains (order.idIntPortfolio())) {
    		throw new IllegalValueException (clazz, method, argument + ".idIntPortfolio", 
    				"Not matching allowed internal portfolios for provided createdBy " + createdBy.id() + " :" + createdBy.tradeablePortfolioIds(), "" + order.idIntPortfolio());
    	}

    	if (order.idExtPortfolio() != null && !createdBy.tradeablePortfolioIds().contains (order.idExtPortfolio())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExtPortfolio",
    				"Not matching allowed external portfolios for provided createdBy " + createdBy.id() + " :" + createdBy.tradeablePortfolioIds(), "" + order.idExtPortfolio());
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
    	if (!createdBy.tradeableInternalPartyIds().contains (order.idInternalBu())) {
    		throw new IllegalValueException (clazz, method, argument + ".idInternalParty", 
    				"Not matching allowed internal parties for provided updatedBy " + updatedBy.id() + " :" + updatedBy.tradeableInternalPartyIds(), "" + order.idInternalBu());
    	}
    	if (!createdBy.tradeableCounterPartyIds().contains (order.idExternalBu())) {
    		throw new IllegalValueException (clazz, method, argument + ".idExternalParty", 
    				"Not matching allowed external parties for provided updatedBy " + updatedBy.id() + " :" + updatedBy.tradeableCounterPartyIds(), "" + order.idExternalBu());
    	}
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
				MockOrderController.class, method , argument + ".idCreditCheckRunStatus", false);

    	TomsService.verifyDefaultReference (creditCheck.idCreditCheckOutcome(),
				Arrays.asList(DefaultReferenceType.CREDIT_CHECK_OUTCOME),
				MockOrderController.class, method , argument + ".idCreditCheckOutcome", false);
    	
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
    		} else {
    			verifyUnchangedStates (clazz, method, argument, availableTransitions.get(0), oldCreditCheck, creditCheck);
    		}
    	}
	}
	
	public static void validateCommentFields(Class clazz, String method,
			String argument, OrderCommentTo newComment, boolean isNew, OrderCommentTo oldComment) {
    	if (isNew) {
    		if (newComment.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + newComment.id());
        	}
    	} else {
    		if (newComment.id() != oldComment.id()) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "" + oldComment.id(), "" + newComment.id());
        	}
    	}
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdfDateTime.parse (newComment.createdAt());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".createdAt", TomsService.DATE_TIME_FORMAT, newComment.createdAt());
		}
		try {
			Date parsedTime = sdfDateTime.parse (newComment.lastUpdate());
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (clazz, method, argument + ".lastUpdate", TomsService.DATE_TIME_FORMAT, newComment.lastUpdate());
		}
		
    	if (!TestUser.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( newComment.idCreatedByUser()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idCreatedByUser" , "User", "" + newComment.idCreatedByUser());
    	}

    	if (!TestUser.asList().stream().map(x -> x.id()).collect(Collectors.toList()).contains( newComment.idUpdatedByUser()) ) {
    		throw new UnknownEntityException (clazz, method, argument + ".idUpdatedByUser" , "User", "" + newComment.idUpdatedByUser());
    	}
	}
	
	private static<T> void verifyUnchangedStates (Class clazz, String method, String argument, ProcessTransitionTo transition, T oldEntity, T newEntity) {
		for (String methodName : transition.unchangeableAttributes()) {
			try {
				// assuming no parameters on method names
				Method m = oldEntity.getClass().getMethod(methodName);
				Object returnValueOld = m.invoke(oldEntity);
				Object returnValueNew = m.invoke(newEntity);
				if (		(returnValueOld == null && returnValueNew != null)
						||  (returnValueOld != null && returnValueNew == null)) {
					throw new IllegalValueException (clazz, method, argument + "." + methodName, "" + returnValueOld, "" + returnValueNew);
				}
				if (returnValueOld == null && returnValueNew == null ) {
					continue; // we can't invoke equals on null
				}
				
				if (returnValueOld instanceof Double) {
					Double oldDouble = (Double) returnValueOld;
					Double newDouble = (Double) returnValueNew;
					if (Math.abs(Math.abs(oldDouble) - Math.abs(newDouble)) > EPSILON) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, "" + oldDouble, "" + newDouble);
					}
				} else if (returnValueOld instanceof Integer) {
					Integer oldInt = (Integer) returnValueOld;
					Integer newInt = (Integer) returnValueNew;
					if (oldInt.intValue() != newInt.intValue()) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, "" + oldInt, "" + newInt);
					}
				} else if (returnValueOld instanceof Collection) {
					Collection oldCollection = (Collection) returnValueOld;
					Collection newCollection = (Collection) returnValueNew;
					if (!oldCollection.containsAll(newCollection) || !newCollection.containsAll(oldCollection)) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, oldCollection.toString(), newCollection.toString());
					}
				} else {
					if (!returnValueOld.equals(returnValueNew)) {
						throw new IllegalValueException (clazz, method, argument + "." + methodName, returnValueOld.toString(), returnValueNew.toString());						
					}
				}
			} catch (NoSuchMethodException e) {
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " does not exist on the objects of type '" + oldEntity.getClass().getName() + "'. Please check the setup of the transition");
			} catch (SecurityException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " is not accessible on the objects of type '" + oldEntity.getClass().getName() + "'. Please check the setup of the transition");
			} catch (IllegalAccessException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " is not accessible on the objects of type '" + oldEntity.getClass().getName() + "'. Please check the setup of the transition");
			} catch (IllegalArgumentException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " in '" + oldEntity.getClass().getName() + "' does take more than one argument. Please check the setup of the transition");
			} catch (InvocationTargetException e) {
				// should not happen
				throw new RuntimeException ("The method '" + methodName + "' as defined in the status transition having ID #" + transition.id()
					+ " in '" + oldEntity.getClass().getName() + "' threw an exception while invoking. The Exception thrown is " + e.getCause());
			} 
		}
	}
}
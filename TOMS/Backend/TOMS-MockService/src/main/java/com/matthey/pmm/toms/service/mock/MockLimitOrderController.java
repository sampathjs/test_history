package com.matthey.pmm.toms.service.mock;


import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;
import com.matthey.pmm.toms.transport.UserTo;
import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.enums.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.DefaultExpirationStatus;
import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.enums.DefaultProcessTransition;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


import com.matthey.pmm.toms.service.TomsLimitOrderService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.exception.IllegalValueException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;

import com.matthey.pmm.toms.service.TomsService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import java.text.SimpleDateFormat;
import java.text.ParseException;

@RestController
public class MockLimitOrderController implements TomsLimitOrderService {
	public static final AtomicInteger ID_COUNTER = new AtomicInteger(10000);
	public static final List<LimitOrderTo> CUSTOM_ORDERS = new CopyOnWriteArrayList<>();

	
	@Override
    @ApiOperation("Retrieval of Limit Order Data")
	public Set<LimitOrderTo> getLimitOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId) {
		Function<LimitOrderTo, Boolean> buySellPredicate = null;
		Function<LimitOrderTo, Boolean> internalPartyPredicate = null;
		Function<LimitOrderTo, Boolean> externalPartyPredicate = null;
		Function<LimitOrderTo, Boolean> minCreationDatePredicate = null;
		Function<LimitOrderTo, Boolean> maxCreationDatePredicate = null;
		
		if (TomsService.verifyDefaultReference (buySellId,
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				this.getClass(), "getLimitOrders","buySellId")) {
			buySellPredicate = x -> (int)x.idBuySell() == (int)buySellId;
		} else {
			buySellPredicate = x -> true;			
		}
		
		if (internalPartyId != null && internalPartyId != 0) {
			internalPartyPredicate = x -> (int)x.idInternalParty() == (int)internalPartyId;
		} else {
			internalPartyPredicate = x -> true;						
		}
		if (externalPartyId != null && externalPartyId != 0) {
			externalPartyPredicate = x -> (int)x.idExternalParty() == (int)externalPartyId;
		} else {
			externalPartyPredicate = x -> true;		
		}
		
		if (minCreatedAtDate != null) {
			minCreationDatePredicate = x -> x.createdAt().compareTo(minCreatedAtDate) >= 0;
		} else {
			minCreationDatePredicate = x -> true;						
		}
		if (maxCreatedAtDate != null) {
			maxCreationDatePredicate = x -> x.createdAt().compareTo(maxCreatedAtDate) <= 0;
		} else {
			maxCreationDatePredicate = x -> true;						
		}
		
		final List<Function<LimitOrderTo, Boolean>> allPredicates = Arrays.asList(
				buySellPredicate, internalPartyPredicate, externalPartyPredicate, minCreationDatePredicate, maxCreationDatePredicate);
		Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_ORDERS.stream());
		
		return new HashSet<>(allDataSources.filter(
				x -> allPredicates.stream().map(y -> y.apply(x)).collect(Collectors.reducing(Boolean.TRUE, Boolean::logicalAnd)))
			.collect(Collectors.toList()));
	}
	
    @ApiOperation("Creation of a Limit Order")
	public int postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder) {
    	// validation checks
    	validateLimitOrderFields (this.getClass(), "postLimitOrder", "newLimitOrder", newLimitOrder, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

    	LimitOrderTo withId = ImmutableLimitOrderTo.copyOf(newLimitOrder)
    			.withId(ID_COUNTER.incrementAndGet())
    			.withLastUpdate(sdfDateTime.format(new Date()));
    	CUSTOM_ORDERS.add (withId);
    	return withId.id();
    }
    
    @ApiOperation("Update of an Limit Order")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder) {
    	// identify the existing limit order
    	List<LimitOrderTo> limitOrders = TestLimitOrder.asList().stream().filter(x -> x.id() == existingLimitOrder.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (limitOrders.size () == 0) {
    		limitOrders = CUSTOM_ORDERS.stream().filter(x -> x.id() == existingLimitOrder.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (limitOrders.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrder", "existingLimitOrder.id" , "Limit Order", "" + existingLimitOrder.id());
    	}
    	validateLimitOrderFields (this.getClass(), "updateLimitOrder", "existingLimitOrder", existingLimitOrder, false, limitOrders.get(0));
    	// everything passed checks, now update limit order
    	if (isEnum) {
        	List<TestLimitOrder> limitOrderEnums = TestLimitOrder.asEnumList().stream().filter(x -> x.getEntity().id() == existingLimitOrder.id()).collect(Collectors.toList());
        	limitOrderEnums.get(0).setEntity(existingLimitOrder);
    	} else {
    		// identification by ID only for LimitOrders, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_ORDERS.remove(existingLimitOrder);
    		CUSTOM_ORDERS.add (existingLimitOrder);
    	}
    }
    
	public static void validateLimitOrderFields (Class clazz, String method, String argument, LimitOrderTo order, boolean newLimitOrder, LimitOrderTo oldLimitOrder) {
		validateOrderFields (clazz, method, argument, order, newLimitOrder, oldLimitOrder);
		// validate input data
		if (order.idOrderType() != DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity().id()) {
			throw new IllegalValueException(clazz, method + ".idOrderType", argument , " = " + DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity().id(), "" + order.idOrderType());
		}		
    	TomsService.verifyDefaultReference (order.idPriceType(),
				Arrays.asList(DefaultReferenceType.PRICE_TYPE),
				MockLimitOrderController.class, method , argument + ".idPriceType");
		
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
				MockLimitOrderController.class, method , argument + ".idYesNoPartFillable");

    	if (order.spotPrice() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".spotPrice", " > 0", "" + order.spotPrice());
    	}
    	
    	TomsService.verifyDefaultReference (order.idStopTriggerType(),
				Arrays.asList(DefaultReferenceType.STOP_TRIGGER_TYPE),
				MockLimitOrderController.class, method , argument + ".idStopTriggerType");
    	
    	TomsService.verifyDefaultReference (order.idCurrencyCrossMetal(),
				Arrays.asList(DefaultReferenceType.CCY_METAL),
				MockLimitOrderController.class, method , argument + ".idCurrencyCrossMetal");

    	if (order.executionLikelihood() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".executionLikelihood", " > 0", "" + order.executionLikelihood());
    	}
    	
    	if (!newLimitOrder) {
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
	
	private static void validateOrderFields (Class clazz, String method, String argument, OrderTo order, boolean newOrder, OrderTo oldOrder) {
    	if (newOrder) {
    		if (order.id() != -1) {
        		throw new IllegalIdException (clazz, method, argument  + ".id", "-1", "" + order.id());
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
				MockLimitOrderController.class, method , argument + ".idBuySell");    
    	
    	TomsService.verifyDefaultReference (order.idMetalCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_METAL),
				MockLimitOrderController.class, method , argument + ".idMetalCurrency");
    	
    	if (order.quantity() <= 0) {
    		throw new IllegalValueException(clazz, method, argument + ".quantity", " > 0", "" + order.quantity());
    	}

    	TomsService.verifyDefaultReference (order.idQuantityUnit(),
				Arrays.asList(DefaultReferenceType.QUANTITY_UNIT),
				MockLimitOrderController.class, method , argument + ".idQuantityUnit");
    	
    	TomsService.verifyDefaultReference (order.idCurrency(),
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY),
				MockLimitOrderController.class, method , argument + ".idCurrency");

    	TomsService.verifyDefaultReference (order.idPaymentPeriod(),
				Arrays.asList(DefaultReferenceType.PAYMENT_PERIOD),
				MockLimitOrderController.class, method , argument + ".idPaymentPeriod");

    	TomsService.verifyDefaultReference (order.idYesNoPhysicalDeliveryRequired(),
				Arrays.asList(DefaultReferenceType.YES_NO),
				MockLimitOrderController.class, method , argument + ".idYesNoPhysicalDeliveryRequired");

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
	}
}
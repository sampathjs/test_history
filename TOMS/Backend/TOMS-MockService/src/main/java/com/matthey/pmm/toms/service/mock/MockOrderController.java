package com.matthey.pmm.toms.service.mock;


import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.service.mock.testdata.TestOrderFill;
import com.matthey.pmm.toms.transport.ImmutableOrderFillTo;
import com.matthey.pmm.toms.transport.OrderFillTo;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
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


import com.matthey.pmm.toms.service.TomsOrderService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

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
public class MockOrderController implements TomsOrderService {
	public static final AtomicInteger ID_COUNTER_FILL = new AtomicInteger(50000);
	public static final List<OrderFillTo> CUSTOM_ORDER_FILLS = new CopyOnWriteArrayList<>();
	public static final AtomicInteger ID_COUNTER_LIMIT_ORDER = new AtomicInteger(10000);
	public static final List<LimitOrderTo> CUSTOM_LIMIT_ORDERS = new CopyOnWriteArrayList<>();
	public static final AtomicInteger ID_COUNTER_REFERENCE_ORDER = new AtomicInteger(10000);
	public static final List<ReferenceOrderTo> CUSTOM_REFERENCE_ORDERS = new CopyOnWriteArrayList<>();
	
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
		Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
		
		return new HashSet<>(allDataSources.filter(
				x -> allPredicates.stream().map(y -> y.apply(x)).collect(Collectors.reducing(Boolean.TRUE, Boolean::logicalAnd)))
			.collect(Collectors.toList()));
	}
	
    @ApiOperation("Retrieval of a single fill for a Limit Order")
    public OrderFillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable int orderFillId) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());

    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId(this.getClass(), "getLimitOrderFill", "limitOrderId", limitOrderId, allDataSources);
    	if (limitOrder.orderFillIds() == null || !limitOrder.orderFillIds().contains(orderFillId) ) {
    		throw new IllegalIdException(this.getClass(), "getLimitOrderFill", "orderFillId", limitOrder.orderFillIds().toString(), "" + orderFillId);
    	}
    	Stream<OrderFillTo> allDataSourcesFills = Stream.concat(TestOrderFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	List<OrderFillTo> fills = allDataSourcesFills
    		.filter(x -> x.id() == orderFillId)
    		.collect(Collectors.toList());
    	if (fills.size() != 1) {
    		throw new IllegalIdException(this.getClass(), "getLimitOrderFill", "orderFillId", limitOrder.orderFillIds().toString(), "" + orderFillId);    		
    	}
    	return fills.get(0);
    }
    
    @ApiOperation("Retrieval of all fills for a Limit Order")
    public Set<OrderFillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId(this.getClass(), "getLimitOrderFills", "limitOrderId", limitOrderId, allDataSources);
    	Stream<OrderFillTo> allOrderFills = Stream.concat(TestOrderFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	
    	if (limitOrder.orderFillIds() != null) {
    		return allOrderFills
    				.filter(x -> limitOrder.orderFillIds().contains(x.id()))
    				.collect(Collectors.toSet());
    	} else {
    		return new HashSet<>();
    	}
    }

	
    @ApiOperation("Creation of a new Limit Order")
	public int postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder) {
    	// validation checks
    	SharedMockLogic.validateLimitOrderFields (this.getClass(), "postLimitOrder", "newLimitOrder", newLimitOrder, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

    	LimitOrderTo withId = ImmutableLimitOrderTo.copyOf(newLimitOrder)
    			.withId(ID_COUNTER_LIMIT_ORDER.incrementAndGet())
    			.withLastUpdate(sdfDateTime.format(new Date()));
    	CUSTOM_LIMIT_ORDERS.add (withId);
    	return withId.id();
    }
    
    @ApiOperation("Update of an existing Limit Order")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder) {
    	// identify the existing limit order
    	List<LimitOrderTo> limitOrders = TestLimitOrder.asList().stream().filter(x -> x.id() == existingLimitOrder.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (limitOrders.size () == 0) {
    		limitOrders = CUSTOM_LIMIT_ORDERS.stream().filter(x -> x.id() == existingLimitOrder.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (limitOrders.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrder", "existingLimitOrder.id" , "Limit Order", "" + existingLimitOrder.id());
    	}
    	SharedMockLogic.validateLimitOrderFields (this.getClass(), "updateLimitOrder", "existingLimitOrder", existingLimitOrder, false, limitOrders.get(0));
    	// everything passed checks, now update limit order
    	if (isEnum) {
        	List<TestLimitOrder> limitOrderEnums = TestLimitOrder.asEnumList().stream().filter(x -> x.getEntity().id() == existingLimitOrder.id()).collect(Collectors.toList());
        	limitOrderEnums.get(0).setEntity(existingLimitOrder);
    	} else {
    		// identification by ID only for LimitOrders, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_LIMIT_ORDERS.remove(existingLimitOrder);
    		CUSTOM_LIMIT_ORDERS.add (existingLimitOrder);
    	}
    }
	
	@Override
    @ApiOperation("Retrieval of Reference Order Data")
	public Set<ReferenceOrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId) {
		Function<ReferenceOrderTo, Boolean> buySellPredicate = null;
		Function<ReferenceOrderTo, Boolean> internalPartyPredicate = null;
		Function<ReferenceOrderTo, Boolean> externalPartyPredicate = null;
		Function<ReferenceOrderTo, Boolean> minCreationDatePredicate = null;
		Function<ReferenceOrderTo, Boolean> maxCreationDatePredicate = null;
		
		if (TomsService.verifyDefaultReference (buySellId,
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				this.getClass(), "getReferenceOrders","buySellId")) {
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
		
		final List<Function<ReferenceOrderTo, Boolean>> allPredicates = Arrays.asList(
				buySellPredicate, internalPartyPredicate, externalPartyPredicate, minCreationDatePredicate, maxCreationDatePredicate);
		Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
		
		return new HashSet<>(allDataSources.filter(
				x -> allPredicates.stream().map(y -> y.apply(x)).collect(Collectors.reducing(Boolean.TRUE, Boolean::logicalAnd)))
			.collect(Collectors.toList()));
	}
	
    @ApiOperation("Retrieval of a the order fill for a Reference Order, if present")
    public OrderFillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId) {
    	Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
    	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId(this.getClass(), "getReferenceOrderFill", "referenceOrderId", referenceOrderId, allDataSources);

    	Stream<OrderFillTo> allDataSources = Stream.concat(TestOrderFill.asList().stream(), CUSTOM_ORDER_ORDER_FILLS.stream());
    	List<OrderFillTo> orderFills = allDataSources
    			.filter( x -> x.id() == referenceOrder.orderFillId())
    			.collect(Collectors.toList());
    	return orderFills.get(0);    	
    }
	
    @ApiOperation("Creation of a new Reference Order")
	public int postReferenceOrder (@ApiParam(value = "The new Reference Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder) {
    	// validation checks
    	SharedMockLogic.validateReferenceOrderFields (this.getClass(), "postReferenceOrder", "newReferenceOrder", newReferenceOrder, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

    	ReferenceOrderTo withId = ImmutableReferenceOrderTo.copyOf(newReferenceOrder)
    			.withId(ID_COUNTER_REFERENCE_ORDER.incrementAndGet())
    			.withLastUpdate(sdfDateTime.format(new Date()));
    	CUSTOM_REFERENCE_ORDERS.add (withId);
    	return withId.id();
    }
    
    @ApiOperation("Update of an existing Reference Order")
	public void updateReferenceOrder (@ApiParam(value = "The Reference Order to update. Order ID has to denote an existing Reference Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder) {
    	// identify the existing Reference order
    	List<ReferenceOrderTo> ReferenceOrders = TestReferenceOrder.asList().stream().filter(x -> x.id() == existingReferenceOrder.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (ReferenceOrders.size () == 0) {
    		ReferenceOrders = CUSTOM_REFERENCE_ORDERS.stream().filter(x -> x.id() == existingReferenceOrder.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (ReferenceOrders.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateReferenceOrder", "existingReferenceOrder.id" , "Reference Order", "" + existingReferenceOrder.id());
    	}
    	SharedMockLogic.validateReferenceOrderFields (this.getClass(), "updateReferenceOrder", "existingReferenceOrder", existingReferenceOrder, false, ReferenceOrders.get(0));
    	// everything passed checks, now update Reference order
    	if (isEnum) {
        	List<TestReferenceOrder> ReferenceOrderEnums = TestReferenceOrder.asEnumList().stream().filter(x -> x.getEntity().id() == existingReferenceOrder.id()).collect(Collectors.toList());
        	ReferenceOrderEnums.get(0).setEntity(existingReferenceOrder);
    	} else {
    		// identification by ID only for ReferenceOrders, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_REFERENCE_ORDERS.remove(existingReferenceOrder);
    		CUSTOM_REFERENCE_ORDERS.add (existingReferenceOrder);
    	}
    }
}
package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsOrderService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockOrderController implements TomsOrderService {
	public static final AtomicInteger ID_COUNTER_ORDER = new AtomicInteger(20000);
	public static final List<OrderTo> CUSTOM_ORDERS = new CopyOnWriteArrayList<>();
	
	@Override
    @ApiOperation("Retrieval of Limit Order Data")
	public Set<OrderTo> getLimitOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId) {
		Function<OrderTo, Boolean> buySellPredicate = null;
		Function<OrderTo, Boolean> internalPartyPredicate = null;
		Function<OrderTo, Boolean> externalPartyPredicate = null;
		Function<OrderTo, Boolean> minCreationDatePredicate = null;
		Function<OrderTo, Boolean> maxCreationDatePredicate = null;
		
		if (TomsService.verifyDefaultReference (buySellId,
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				this.getClass(), "getLimitOrders","buySellId", false)) {
			buySellPredicate = x -> (int)x.idBuySell() == (int)buySellId;
		} else {
			buySellPredicate = x -> true;
		}
		
		if (internalPartyId != null && internalPartyId != 0) {
			internalPartyPredicate = x -> (int)x.idInternalBu() == (int)internalPartyId;
		} else {
			internalPartyPredicate = x -> true;						
		}
		if (externalPartyId != null && externalPartyId != 0) {
			externalPartyPredicate = x -> (int)x.idExternalBu() == (int)externalPartyId;
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
		
		final List<Function<OrderTo, Boolean>> allPredicates = Arrays.asList(
				buySellPredicate, internalPartyPredicate, externalPartyPredicate, minCreationDatePredicate, maxCreationDatePredicate);
		Stream<OrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_ORDERS.stream());
		
		return new HashSet<>(allDataSources.filter(
				x -> allPredicates.stream().map(y -> y.apply(x)).collect(Collectors.reducing(Boolean.TRUE, Boolean::logicalAnd)))
			.collect(Collectors.toList()));
	}
	
    @ApiOperation("Creation of a new Limit Order")
	public int postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder) {
    	// validation checks
    	SharedMockLogic.validateLimitOrderFields (this.getClass(), "postLimitOrder", "newLimitOrder", newLimitOrder, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

    	LimitOrderTo withId = ImmutableLimitOrderTo.copyOf(newLimitOrder)
    			.withId(ID_COUNTER_ORDER.incrementAndGet())
    			.withLastUpdate(sdfDateTime.format(new Date()));
    	CUSTOM_ORDERS.add (withId);
    	return withId.id();
    }
    
    @ApiOperation("Update of an existing Limit Order")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder) {
    	// identify the existing limit order
    	List<OrderTo> limitOrders = TestLimitOrder.asList().stream().filter(x -> x.id() == existingLimitOrder.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (limitOrders.size () == 0) {
    		limitOrders = CUSTOM_ORDERS.stream().filter(x -> x.id() == existingLimitOrder.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (limitOrders.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrder", "existingLimitOrder.id" , "Limit Order", "" + existingLimitOrder.id());
    	}    	
    	SharedMockLogic.validateLimitOrderFields (this.getClass(), "updateLimitOrder", "existingLimitOrder", existingLimitOrder, false, limitOrders.get(0));
    	// everything passed checks, now update limit order
    	LimitOrderTo versionUpdated = ImmutableLimitOrderTo.copyOf(existingLimitOrder)
    			.withVersion(existingLimitOrder.version()+1);
    	if (isEnum) {
        	List<TestLimitOrder> limitOrderEnums = TestLimitOrder.asEnumList().stream().filter(x -> x.getEntity().id() == existingLimitOrder.id()).collect(Collectors.toList());
        	limitOrderEnums.get(0).setEntity(versionUpdated);
    	} else {
    		// identification by ID only for LimitOrders, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_ORDERS.remove(versionUpdated);
    		CUSTOM_ORDERS.add (versionUpdated);
    	}
    }
	
	@Override
    @ApiOperation("Retrieval of Reference Order Data")
	public Set<OrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId) {
		Function<OrderTo, Boolean> buySellPredicate = null;
		Function<OrderTo, Boolean> internalPartyPredicate = null;
		Function<OrderTo, Boolean> externalPartyPredicate = null;
		Function<OrderTo, Boolean> minCreationDatePredicate = null;
		Function<OrderTo, Boolean> maxCreationDatePredicate = null;
		
		if (TomsService.verifyDefaultReference (buySellId,
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				this.getClass(), "getReferenceOrders","buySellId", false)) {
			buySellPredicate = x -> (int)x.idBuySell() == (int)buySellId;
		} else {
			buySellPredicate = x -> true;			
		}
		
		if (internalPartyId != null && internalPartyId != 0) {
			internalPartyPredicate = x -> (int)x.idInternalBu() == (int)internalPartyId;
		} else {
			internalPartyPredicate = x -> true;						
		}
		if (externalPartyId != null && externalPartyId != 0) {
			externalPartyPredicate = x -> (int)x.idExternalBu() == (int)externalPartyId;
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
		
		final List<Function<OrderTo, Boolean>> allPredicates = Arrays.asList(
				buySellPredicate, internalPartyPredicate, externalPartyPredicate, minCreationDatePredicate, maxCreationDatePredicate);
		Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_ORDERS.stream());
		
		return new HashSet<>(allDataSources.filter(
				x -> allPredicates.stream().map(y -> y.apply(x)).collect(Collectors.reducing(Boolean.TRUE, Boolean::logicalAnd)))
			.collect(Collectors.toList()));
	}
    
    @ApiOperation("Creation of a new Reference Order")
	public int postReferenceOrder (@ApiParam(value = "The new Reference Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder) {
    	// validation checks
    	SharedMockLogic.validateReferenceOrderFields (this.getClass(), "postReferenceOrder", "newReferenceOrder", newReferenceOrder, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

    	ReferenceOrderTo withId = ImmutableReferenceOrderTo.copyOf(newReferenceOrder)
    			.withId(ID_COUNTER_ORDER.incrementAndGet())
    			.withLastUpdate(sdfDateTime.format(new Date()));
    	CUSTOM_ORDERS.add (withId);
    	return withId.id();
    }
    
    @ApiOperation("Update of an existing Reference Order")
	public void updateReferenceOrder (@ApiParam(value = "The Reference Order to update. Order ID has to denote an existing Reference Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder) {
    	// identify the existing Reference order
    	List<OrderTo> referenceOrders = TestReferenceOrder.asList().stream().filter(x -> x.id() == existingReferenceOrder.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (referenceOrders.size () == 0) {
    		referenceOrders = CUSTOM_ORDERS.stream().filter(x -> x.id() == existingReferenceOrder.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (referenceOrders.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateReferenceOrder", "existingReferenceOrder.id" , "Reference Order", "" + existingReferenceOrder.id());
    	}
    	SharedMockLogic.validateReferenceOrderFields (this.getClass(), "updateReferenceOrder", "existingReferenceOrder", existingReferenceOrder, false, referenceOrders.get(0));
    	ReferenceOrderTo withUpdatedVersion = ImmutableReferenceOrderTo.copyOf(existingReferenceOrder)
    			.withVersion(existingReferenceOrder.version()+1);
    	// everything passed checks, now update Reference order
    	if (isEnum) {
        	List<TestReferenceOrder> ReferenceOrderEnums = TestReferenceOrder.asEnumList().stream().filter(x -> x.getEntity().id() == withUpdatedVersion.id()).collect(Collectors.toList());
        	ReferenceOrderEnums.get(0).setEntity(withUpdatedVersion);
    	} else {
    		// identification by ID only for ReferenceOrders, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_ORDERS.remove(withUpdatedVersion);
    		CUSTOM_ORDERS.add (withUpdatedVersion);
    	}
    }
}
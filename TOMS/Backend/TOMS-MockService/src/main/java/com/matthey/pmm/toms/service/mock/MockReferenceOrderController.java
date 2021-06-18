package com.matthey.pmm.toms.service.mock;


import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
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


import com.matthey.pmm.toms.service.TomsReferenceOrderService;

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
public class MockReferenceOrderController implements TomsReferenceOrderService {
	public static final AtomicInteger ID_COUNTER = new AtomicInteger(10000);
	public static final List<ReferenceOrderTo> CUSTOM_ORDERS = new CopyOnWriteArrayList<>();
	
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
		Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_ORDERS.stream());
		
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
    			.withId(ID_COUNTER.incrementAndGet())
    			.withLastUpdate(sdfDateTime.format(new Date()));
    	CUSTOM_ORDERS.add (withId);
    	return withId.id();
    }
    
    @ApiOperation("Update of an existing Reference Order")
	public void updateReferenceOrder (@ApiParam(value = "The Reference Order to update. Order ID has to denote an existing Reference Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder) {
    	// identify the existing Reference order
    	List<ReferenceOrderTo> ReferenceOrders = TestReferenceOrder.asList().stream().filter(x -> x.id() == existingReferenceOrder.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (ReferenceOrders.size () == 0) {
    		ReferenceOrders = CUSTOM_ORDERS.stream().filter(x -> x.id() == existingReferenceOrder.id()).collect(Collectors.toList());
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
    		CUSTOM_ORDERS.remove(existingReferenceOrder);
    		CUSTOM_ORDERS.add (existingReferenceOrder);
    	}
    }
}
package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsOrderService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestCreditCheck;
import com.matthey.pmm.toms.service.mock.testdata.TestFill;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableFillTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockOrderController implements TomsOrderService {
	public static final AtomicInteger ID_COUNTER_FILL = new AtomicInteger(50000);
	public static final List<FillTo> CUSTOM_ORDER_FILLS = new CopyOnWriteArrayList<>();
	
	public static final AtomicInteger ID_COUNTER_LIMIT_ORDER = new AtomicInteger(10000);
	public static final List<LimitOrderTo> CUSTOM_LIMIT_ORDERS = new CopyOnWriteArrayList<>();
	
	public static final AtomicInteger ID_COUNTER_REFERENCE_ORDER = new AtomicInteger(20000);
	public static final List<ReferenceOrderTo> CUSTOM_REFERENCE_ORDERS = new CopyOnWriteArrayList<>();

	public static final AtomicInteger ID_COUNTER_CREDIT_LIMIT_CHECK = new AtomicInteger(10000);
	public static final List<CreditCheckTo> CUSTOM_CREDIT_LIMIT_CHECKS = new CopyOnWriteArrayList<>();

	
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
    public FillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable int fillId) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());

    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId(this.getClass(), "getLimitOrderFill", "limitOrderId", limitOrderId, allDataSources);
    	if (limitOrder.fillIds() == null || !limitOrder.fillIds().contains(fillId) ) {
    		throw new IllegalIdException(this.getClass(), "getLimitOrderFill", "fillId", limitOrder.fillIds().toString(), "" + fillId);
    	}
    	Stream<FillTo> allDataSourcesFills = Stream.concat(TestFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	List<FillTo> fills = allDataSourcesFills
    		.filter(x -> x.id() == fillId)
    		.collect(Collectors.toList());
    	if (fills.size() != 1) {
    		throw new IllegalIdException(this.getClass(), "getLimitOrderFill", "fillId", limitOrder.fillIds().toString(), "" + fillId);    		
    	}
    	return fills.get(0);
    }
    
    @ApiOperation("Retrieval of all fills for a Limit Order")
    public Set<FillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId(this.getClass(), "getLimitOrderFills", "limitOrderId", limitOrderId, allDataSources);
    	Stream<FillTo> allOrderFills = Stream.concat(TestFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	
    	if (limitOrder.fillIds() != null) {
    		return allOrderFills
    				.filter(x -> limitOrder.fillIds().contains(x.id()))
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
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    public int postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId (this.getClass(), "getLimitOrderFill", "limitOrderId", limitOrderId, allDataSources);
    	// validation checks
    	SharedMockLogic.validateFillFields (this.getClass(), "postLimitOrderFill", "newOrderFill", newOrderFill, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

		FillTo withId = ImmutableFillTo.copyOf(newOrderFill)
    			.withId(ID_COUNTER_FILL.incrementAndGet())
    			.withLastUpdateDateTime(sdfDateTime.format(new Date()));
		CUSTOM_ORDER_FILLS.add (withId);		
		
		List<TestLimitOrder> enumList = TestLimitOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == limitOrderId)
			.collect(Collectors.toList());

		List<Integer> newfillIds = new ArrayList<>(limitOrder.fillIds().size()+1);
		newfillIds.addAll (limitOrder.fillIds());
		newfillIds.add(withId.id());

		LimitOrderTo updatedLimitOrder = ImmutableLimitOrderTo.copyOf(limitOrder)
				.withFillIds(newfillIds)
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestLimitOrder order = enumList.get(0);
	    	order.setEntity (updatedLimitOrder);
		} else {
			CUSTOM_LIMIT_ORDERS.remove(limitOrder);			
			CUSTOM_LIMIT_ORDERS.add(updatedLimitOrder);
		}
		return withId.id();
    }
    
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
    public Set<CreditCheckTo> getCreditCheckLimitOrders (
    		@ApiParam(value = "The order ID of the order the Credit Check object is to be retrieved from", example = "1") @PathVariable int limitOrderId) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId(this.getClass(), "getCreditCheckLimitOrders", "limitOrderId", limitOrderId, allDataSources);
    	
    	if (limitOrder.creditChecksIds() == null) {
    		return null;
    	}
	
    	Stream<CreditCheckTo> allDataSourcesFill = Stream.concat(TestCreditCheck.asList().stream(), CUSTOM_CREDIT_LIMIT_CHECKS.stream());
    	Set<CreditCheckTo> creditChecks = allDataSourcesFill
    			.filter( x -> limitOrder.creditChecksIds().contains(x.id()))
    			.collect(Collectors.toSet());
    	return creditChecks;    	
    }
    
    @ApiOperation("Creation of a new Credit Check for a Reference Order")
    public int postLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new Credit Check. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
       	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
       	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId (this.getClass(), "postLimitOrderCreditCheck", "limitOrderId", limitOrderId, allDataSources);
    	// validation checks
    	SharedMockLogic.validateCreditCheckFields (this.getClass(), "postLimitOrderCreditCheck", "newCreditCheck", newCreditCheck, true, null);

		CreditCheckTo withId = ImmutableCreditCheckTo.copyOf(newCreditCheck)
    			.withId(ID_COUNTER_CREDIT_LIMIT_CHECK.incrementAndGet());
		CUSTOM_CREDIT_LIMIT_CHECKS.add (withId);

		List<TestLimitOrder> enumList = TestLimitOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == limitOrderId)
			.collect(Collectors.toList());
		
		Set<Integer> newCreditCheckIds = new HashSet<>(limitOrder.creditChecksIds());
		newCreditCheckIds.add(withId.id());
		
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		LimitOrderTo updatedLimitOrder = ImmutableLimitOrderTo.copyOf(limitOrder)
				.withCreditChecksIds(newCreditCheckIds)
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestLimitOrder order = enumList.get(0);
	    	order.setEntity (updatedLimitOrder);
		} else {
			CUSTOM_LIMIT_ORDERS.remove(limitOrder);			
			CUSTOM_LIMIT_ORDERS.add(updatedLimitOrder);
		}
		return withId.id();
    	
    }

    @ApiOperation("Update of the Credit Check for a Reference Order")
    public void updateLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1") @PathVariable int creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck) {
		Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
		LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId (this.getClass(), "updateLimitOrderCreditCheck", "limitOrderId", limitOrderId, allDataSources);

    	if (creditCheckId != existingCreditCheck.id()) {
    		throw new IllegalIdException(this.getClass(), "updateLimitOrderCreditCheck", "existingCreditCheck/creditCheckId",
    				"" + creditCheckId, "" + existingCreditCheck.id());
    	}
    	
    	if (limitOrder.creditChecksIds() == null || 
    			!limitOrder.creditChecksIds().contains(existingCreditCheck.id())) {
       		throw new IllegalStateException (this.getClass(), "updateLimitOrderCreditCheck", "creditCheckId", 
       				"The provided credit check ID does not match any of the existing credit check IDs",
       				"Reference Order #" + creditCheckId,
       				"The existing CreditCheck must be one of the following " + limitOrder.creditChecksIds());
    	}
    	
       	// identify the existing credit check
    	List<CreditCheckTo> creditChecks = TestCreditCheck.asList().stream().filter(x -> x.id() == existingCreditCheck.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (creditChecks.size () == 0) {
    		creditChecks = CUSTOM_CREDIT_LIMIT_CHECKS.stream().filter(x -> x.id() == existingCreditCheck.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (creditChecks.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrderCreditCheck", "existingCreditCheck.id" , "Limit Order", "" + existingCreditCheck.id());
    	}
    	    	
    	SharedMockLogic.validateCreditCheckFields (this.getClass(), "updateLimitOrderCreditCheck", "existingCreditCheck", existingCreditCheck, false, creditChecks.get(0));
    	// everything passed checks, now update credit check data
    	if (isEnum) {
        	List<TestCreditCheck> creditCheckEnums = TestCreditCheck.asEnumList().stream().filter(x -> x.getEntity().id() == existingCreditCheck.id()).collect(Collectors.toList());
        	creditCheckEnums.get(0).setEntity(existingCreditCheck);
    	} else {
    		// identification by ID only for credit checks, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_CREDIT_LIMIT_CHECKS.remove(existingCreditCheck);
    		CUSTOM_CREDIT_LIMIT_CHECKS.add (existingCreditCheck);
    	}    	
    }

    
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
    public CreditCheckTo getCreditCheckLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the Credit Check is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to retrieve ", example = "1") @PathVariable int creditCheck) {
    	Stream<LimitOrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), CUSTOM_LIMIT_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateLimitOrderId(this.getClass(), "getCreditCheckLimitOrder", "limitOrderId", limitOrderId, allDataSources);
    	
    	if (limitOrder.creditChecksIds() == null) {
    		throw new IllegalIdException(this.getClass(), "getCreditCheckLimitOrder", "creditCheck", "<order does not have limit checks>", "" + limitOrderId);
    	}
	
    	Stream<CreditCheckTo> allDataSourcesCreditCheck = Stream.concat(TestCreditCheck.asList().stream(), CUSTOM_CREDIT_LIMIT_CHECKS.stream());
    	List<CreditCheckTo> creditChecks = allDataSourcesCreditCheck
    			.filter( x -> limitOrder.creditChecksIds().contains(x.id()) && x.id() == creditCheck)
    			.collect(Collectors.toList());
    	if (creditChecks.size() == 1) {
        	return creditChecks.get(0);    		
    	} else {
    		return null;
    	}
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
	
    @ApiOperation("Retrieval of a the fill for a Reference Order, if present")
    public FillTo getReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId) {
    	Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
    	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId(this.getClass(), "getReferenceOrderFill", "referenceOrderId", referenceOrderId, allDataSources);
    	
    	if (referenceOrder.fillId() == null) {
    		return null;
    	}
    	
    	Stream<FillTo> allDataSourcesFill = Stream.concat(TestFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	List<FillTo> orderFills = allDataSourcesFill
    			.filter( x -> x.id() == referenceOrder.fillId())
    			.collect(Collectors.toList());
    	return orderFills.get(0);    	
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    public int postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
       	Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
       	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId (this.getClass(), "postLimitOrderFill", "referenceOrderId", referenceOrderId, allDataSources);
       	if (referenceOrder.fillId() != null && referenceOrder.fillId() > 0) {
       		throw new IllegalStateException (this.getClass(), "postReferenceOrderFill", "newOrderFill", 
       				"fill for Reference Order having fill already installed",
       				"Reference Order #" + referenceOrderId + " already assigned to fill ID #" + referenceOrder.fillId(),
       				"fill not assigned");
       	}
    	// validation checks
    	SharedMockLogic.validateFillFields (this.getClass(), "postReferenceOrderFill", "newOrderFill", newOrderFill, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

		FillTo withId = ImmutableFillTo.copyOf(newOrderFill)
    			.withId(ID_COUNTER_FILL.incrementAndGet())
    			.withLastUpdateDateTime(sdfDateTime.format(new Date()));
		CUSTOM_ORDER_FILLS.add (withId);		
		
		List<TestReferenceOrder> enumList = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == referenceOrderId)
			.collect(Collectors.toList());

		ReferenceOrderTo updatedReferenceOrder = ImmutableReferenceOrderTo.copyOf(referenceOrder)
				.withFillId(withId.id())
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestReferenceOrder order = enumList.get(0);
	    	order.setEntity (updatedReferenceOrder);
		} else {
			CUSTOM_REFERENCE_ORDERS.remove(referenceOrder);			
			CUSTOM_REFERENCE_ORDERS.add(updatedReferenceOrder);
		}
		return withId.id();    	
    }
    
    @ApiOperation("Creation of a new Credit Check for a Reference Order")
    public int postReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new Credit Check . ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
       	Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
       	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId (this.getClass(), "postReferenceOrderCreditCheck", "referenceOrderId", referenceOrderId, allDataSources);
    	// validation checks
    	SharedMockLogic.validateCreditCheckFields (this.getClass(), "postReferenceOrderCreditCheck", "newCreditCheck", newCreditCheck, true, null);

		CreditCheckTo withId = ImmutableCreditCheckTo.copyOf(newCreditCheck)
    			.withId(ID_COUNTER_CREDIT_LIMIT_CHECK.incrementAndGet());
		CUSTOM_CREDIT_LIMIT_CHECKS.add (withId);

		List<TestReferenceOrder> enumList = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == referenceOrderId)
			.collect(Collectors.toList());
		
		Set<Integer> newCreditCheckIds = new HashSet<>(referenceOrder.creditChecksIds());
		newCreditCheckIds.add(withId.id());
		
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		ReferenceOrderTo updatedReferenceOrder = ImmutableReferenceOrderTo.copyOf(referenceOrder)
				.withCreditChecksIds(newCreditCheckIds)
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestReferenceOrder order = enumList.get(0);
	    	order.setEntity (updatedReferenceOrder);
		} else {
			CUSTOM_REFERENCE_ORDERS.remove(referenceOrder);			
			CUSTOM_REFERENCE_ORDERS.add(updatedReferenceOrder);
		}
		return withId.id();
    }
    
    @ApiOperation("Update of the Credit Check for a Reference Order")
    public void updateReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1") @PathVariable int creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck) {
		Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
    	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId (this.getClass(), "updateReferenceOrderCreditCheck", "referenceOrderId", referenceOrderId, allDataSources);

    	if (creditCheckId != existingCreditCheck.id()) {
    		throw new IllegalIdException(this.getClass(), "updateReferenceOrderCreditCheck", "existingCreditCheck/referenceOrderId",
    				"" + creditCheckId, "" + existingCreditCheck.id());
    	}
    	
    	if (referenceOrder.creditChecksIds() == null || 
    			!referenceOrder.creditChecksIds().contains(existingCreditCheck.id())) {
       		throw new IllegalStateException (this.getClass(), "updateReferenceOrderCreditCheck", "existingCreditCheck", 
       				"The provided credit check ID does not match any of the existing credit check IDs",
       				"Reference Order #" + referenceOrderId,
       				"The existingCreditCheck must be one of the following " + referenceOrder.creditChecksIds());
    	}
    	
       	// identify the existing credit check
    	List<CreditCheckTo> creditChecks = TestCreditCheck.asList().stream().filter(x -> x.id() == existingCreditCheck.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (creditChecks.size () == 0) {
    		creditChecks = CUSTOM_CREDIT_LIMIT_CHECKS.stream().filter(x -> x.id() == existingCreditCheck.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (creditChecks.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateReferenceOrderCreditCheck", "existingCreditCheck.id" , "Limit Order", "" + existingCreditCheck.id());
    	}
    	    	
    	SharedMockLogic.validateCreditCheckFields (this.getClass(), "updateReferenceOrderCreditCheck", "existingCreditCheck", existingCreditCheck, false, creditChecks.get(0));
    	// everything passed checks, now update crdit check data
    	if (isEnum) {
        	List<TestCreditCheck> creditCheckEnums = TestCreditCheck.asEnumList().stream().filter(x -> x.getEntity().id() == existingCreditCheck.id()).collect(Collectors.toList());
        	creditCheckEnums.get(0).setEntity(existingCreditCheck);
    	} else {
    		// identification by ID only for credit checks, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_CREDIT_LIMIT_CHECKS.remove(existingCreditCheck);
    		CUSTOM_CREDIT_LIMIT_CHECKS.add (existingCreditCheck);
    	}
    }

    
    @ApiOperation("Retrieval of the Credit Check data for a Reference Order")
    public Set<CreditCheckTo> getCreditChecksReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "1") @PathVariable int referenceOrderId) {
    	Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
    	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId(this.getClass(), "getCreditChecksReferenceOrders", "limitOrderId", referenceOrderId, allDataSources);
    	
    	if (referenceOrder.creditChecksIds() == null) {
    		return new HashSet<>();
    	}
	
    	Stream<CreditCheckTo> allDataSourcesFill = Stream.concat(TestCreditCheck.asList().stream(), CUSTOM_CREDIT_LIMIT_CHECKS.stream());
    	Set<CreditCheckTo> creditChecks = allDataSourcesFill
    			.filter( x -> referenceOrder.creditChecksIds().contains(x.id()))
    			.collect(Collectors.toSet());
    	return creditChecks;
    }
    
    @ApiOperation("Retrieval of the Credit Check data for a Reference Order")
    public CreditCheckTo getCreditChecksReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1") @PathVariable int creditCheck) {
    	Stream<ReferenceOrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_REFERENCE_ORDERS.stream());
    	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId(this.getClass(), "getCreditChecksReferenceOrder", "referenceOrderId", referenceOrderId, allDataSources);
    	
    	if (referenceOrder.creditChecksIds() == null) {
    		throw new IllegalIdException(this.getClass(), "getCreditChecksReferenceOrder", "creditCheck", "<order does not have limit checks>", "" + referenceOrderId);
    	}
	
    	Stream<CreditCheckTo> allDataSourcesCreditCheck = Stream.concat(TestCreditCheck.asList().stream(), CUSTOM_CREDIT_LIMIT_CHECKS.stream());
    	List<CreditCheckTo> creditChecks = allDataSourcesCreditCheck
    			.filter( x -> referenceOrder.creditChecksIds().contains(x.id()) && x.id() == creditCheck)
    			.collect(Collectors.toList());
    	if (creditChecks.size() == 1) {
        	return creditChecks.get(0);    		
    	} else {
    		return null;
    	}
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
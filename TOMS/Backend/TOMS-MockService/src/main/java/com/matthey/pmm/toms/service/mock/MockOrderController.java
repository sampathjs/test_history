package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsOrderService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
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
	@Autowired
	protected Validator validator;
	
	@Autowired 
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;
	
	@Autowired
	protected LimitOrderConverter limitOrderConverter;
	
	@Autowired
	protected ReferenceOrderConverter referenceOrderConverter;	
	
	public static final AtomicLong ID_COUNTER_ORDER = new AtomicLong(20000);
	public static final List<OrderTo> CUSTOM_ORDERS = new CopyOnWriteArrayList<>();
	
	@Override
    @ApiOperation("Retrieval of Limit Order Data")
	public Set<OrderTo> getLimitOrders (
			@ApiParam(value = "The internal BU IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20006", required = false) @RequestParam(required=false) Long internalBuId,
			@ApiParam(value = "The external BU IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20022", required = false) @RequestParam(required=false) Long externalBuId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Long buySellId,
			@ApiParam(value = "Version ID, null = latest order version", example = "1", required = false) @RequestParam(required=false) Integer versionId) {
		Optional<Party> intBu = validator.verifyParty(internalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT), getClass(), "getLimitOrders", "internalBuId", true);
		Optional<Party> extBu = validator.verifyParty(externalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT), getClass(), "getLimitOrders", "externalBuId", true);
		Date minCreatedAt = validator.verifyDateTime (minCreatedAtDate, getClass(), "getLimitOrders", "minCreatedAtDate");
		Date maxCreatedAt = validator.verifyDateTime (maxCreatedAtDate, getClass(), "getLimitOrders", "maxCreatedAtDate");
		Optional<Reference> buySell = validator.verifyDefaultReference(buySellId, Arrays.asList(DefaultReferenceType.BUY_SELL), getClass(), "getLimitOrders", "buySellId", true); 
		
		List<LimitOrder> matchingOrders = limitOrderRepo.findByOrderIdAndOptionalParameters(intBu.orElse(null), extBu.orElse(null), buySell.orElse(null), minCreatedAt, maxCreatedAt, versionId);
		return matchingOrders.stream()
				.map(x -> limitOrderConverter.toTo(x))
				.collect(Collectors.toSet());
	}
	
    @ApiOperation("Creation of a new Limit Order")
	public long postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder) {
    	// validation checks
    	validator.validateLimitOrderFields (this.getClass(), "postLimitOrder", "newLimitOrder", newLimitOrder, true, null);
	
		LimitOrder managedEntity = limitOrderConverter.toManagedEntity(newLimitOrder);
		managedEntity.setCreatedAt(new Date());
		managedEntity = limitOrderRepo.save(managedEntity);
    	return managedEntity.getOrderId();
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
//        	List<TestLimitOrder> limitOrderEnums = TestLimitOrder.asEnumList().stream().filter(x -> x.getEntity().id() == existingLimitOrder.id()).collect(Collectors.toList());
//        	limitOrderEnums.get(0).setEntity(versionUpdated);
    		CUSTOM_ORDERS.add (versionUpdated);
    	} else {
    		CUSTOM_ORDERS.add (versionUpdated);
    	}
    }
	
	@Override
    @ApiOperation("Retrieval of Reference Order Data")
	public Set<OrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Long longernalPartyId,
			@ApiParam(value = "The external party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Long externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Long buySellId,
			@ApiParam(value = "Version ID, -1 = all limit orders", example = "1", required = false) @RequestParam(required=false) Integer versionId) {
		Function<OrderTo, Boolean> buySellPredicate = null;
		Function<OrderTo, Boolean> longernalPartyPredicate = null;
		Function<OrderTo, Boolean> externalPartyPredicate = null;
		Function<OrderTo, Boolean> minCreationDatePredicate = null;
		Function<OrderTo, Boolean> maxCreationDatePredicate = null;
		Function<OrderTo, Boolean> versionPredicate = null;
		
		if (TomsService.verifyDefaultReference (buySellId,
				Arrays.asList(DefaultReferenceType.BUY_SELL),
				this.getClass(), "getReferenceOrders","buySellId", false)) {
			buySellPredicate = x -> (long)x.idBuySell() == (long)buySellId;
		} else {
			buySellPredicate = x -> true;			
		}
		
		if (longernalPartyId != null && longernalPartyId != 0) {
			longernalPartyPredicate = x -> (long)x.idInternalBu() == (long)longernalPartyId;
		} else {
			longernalPartyPredicate = x -> true;						
		}
		if (externalPartyId != null && externalPartyId != 0) {
			externalPartyPredicate = x -> (long)x.idExternalBu() == (long)externalPartyId;
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
		
		if (versionId != null && versionId != -1) {
			versionPredicate = x -> x.version() == versionId;
		} else {
			versionPredicate = x -> true;						
		}
		
		final List<Function<OrderTo, Boolean>> allPredicates = Arrays.asList(
				buySellPredicate, longernalPartyPredicate, externalPartyPredicate, minCreationDatePredicate, maxCreationDatePredicate,
				versionPredicate);
		Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), CUSTOM_ORDERS.stream());
		
		return new HashSet<>(allDataSources.filter(
				x -> allPredicates.stream().map(y -> y.apply(x)).collect(Collectors.reducing(Boolean.TRUE, Boolean::logicalAnd)))
			.collect(Collectors.toList()));
	}
    
    @ApiOperation("Creation of a new Reference Order")
	public long postReferenceOrder (@ApiParam(value = "The new Reference Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder) {
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
//        	List<TestReferenceOrder> ReferenceOrderEnums = TestReferenceOrder.asEnumList().stream().filter(x -> x.getEntity().id() == withUpdatedVersion.id()).collect(Collectors.toList());
//        	ReferenceOrderEnums.get(0).setEntity(withUpdatedVersion);
    		CUSTOM_ORDERS.add (withUpdatedVersion);
    	} else {
    		// identification by ID only for ReferenceOrders, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_ORDERS.add (withUpdatedVersion);
    	}
    }
    
    @ApiOperation("Retrieve the value of an attribute of the provided Limit Order based on the other fields of the order")
    public String getAttributeValueLimitOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "settleDate", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) LimitOrderTo orderTemplate) {
    	return TomsService.applyAttributeCalculation(orderTemplate, attributeName);
    }

	@ApiOperation("Retrieve the value of an attribute of the provided Reference Order based on the other fields of the order")
    public String getAttributeValueReferenceOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "settleDate", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) ReferenceOrderTo orderTemplate) {
    	return TomsService.applyAttributeCalculation(orderTemplate, attributeName);
    }
}
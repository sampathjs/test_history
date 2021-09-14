package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsCreditCheckService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.testdata.TestCreditCheck;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockCreditCheckService implements TomsCreditCheckService {
	@Autowired
	protected Validator validator;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;

	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;
	
	@Autowired
	protected CreditCheckRepository creditCheckRepo;
	
	@Autowired
	protected CreditCheckConverter creditCheckConverter;
	
	
	public static final AtomicLong ID_COUNTER_CREDIT_LIMIT_CHECK = new AtomicLong(10000);
	public static final List<CreditCheckTo> CUSTOM_CREDIT_LIMIT_CHECKS = new CopyOnWriteArrayList<>();
    
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
    public Set<CreditCheckTo> getCreditCheckLimitOrders (
    		@ApiParam(value = "The order ID of the order the Credit Check object is to be retrieved from", example = "1000001") @PathVariable long limitOrderId) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "getCreditCheckLimitOrders", "limitOrderId", false);
    	    	
    	if (limitOrder.get().getCreditChecks() == null || limitOrder.get().getCreditChecks().size() == 0) {
    		return null;
    	}
	
    	Set<CreditCheckTo> creditChecks = limitOrder.get().getCreditChecks().stream()
    			.map(x -> creditCheckConverter.toTo(x))
    			.collect(Collectors.toSet());
    	return creditChecks;
    }
    
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
    public CreditCheckTo getCreditCheckLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the Credit Check is to be retrieved from", example = "1000001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to retrieve ", example = "1000000") @PathVariable long creditCheckId) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "getCreditCheckLimitOrder", "limitOrderId", false);
    	
    	if (limitOrder.get().getCreditChecks() == null || limitOrder.get().getCreditChecks().size() == 0) {
    		return null;
    	}
    	
    	Optional<CreditCheck> creditCheck =  validator.verifyCreditCheck (limitOrder.get(), creditCheckId, getClass(), "getCreditCheckLimitOrder", "limitOrderId", false);
    	
       	return creditCheckConverter.toTo(creditCheck.get()); 
    }
    
    @ApiOperation("Creation of a new Credit Check for a Limit Order")
    public long postLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1000001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new Credit Check. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "getCreditCheckLimitOrder", "limitOrderId", false);

    	// validation checks
    	validator.validateCreditCheckFields(this.getClass(), "postLimitOrderCreditCheck", "newCreditCheck", newCreditCheck, true, null);

    	CreditCheck persisted = creditCheckConverter.toManagedEntity(newCreditCheck);		
		limitOrder.get().getCreditChecks().add(persisted);
		limitOrder.get().setLastUpdate(new Date());
		limitOrderRepo.save(limitOrder.get());
		return persisted.getId();    	
    }

    @ApiOperation("Update of the Credit Check for a Limit Order")
    public void updateLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1000001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000000") @PathVariable long creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck) {

    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "updateLimitOrderCreditCheck", "limitOrderId", false);
    	Optional<CreditCheck> oldCreditCheck = validator.verifyCreditCheck(limitOrder.get(), creditCheckId, getClass(),"updateLimitOrderCreditCheck", "creditCheckId", false);
    	
    	validator.validateCreditCheckFields(getClass(), "updateLimitOrderCreditCheck", "limitOrderId", existingCreditCheck, false,  creditCheckConverter.toTo(oldCreditCheck.get()));
    	CreditCheck needsToBePersisted = creditCheckConverter.toManagedEntity(existingCreditCheck);
    	creditCheckRepo.save(needsToBePersisted);
    }
    
    @ApiOperation("Creation of a new Credit Check for a Reference Order")
    public long postReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new Credit Check . ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
       	Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
       	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId (this.getClass(), "postReferenceOrderCreditCheck", "referenceOrderId", referenceOrderId, allDataSources);
    	// validation checks
    	SharedMockLogic.validateCreditCheckFields (this.getClass(), "postReferenceOrderCreditCheck", "newCreditCheck", newCreditCheck, true, null);

		CreditCheckTo withId = ImmutableCreditCheckTo.copyOf(newCreditCheck)
    			.withId(ID_COUNTER_CREDIT_LIMIT_CHECK.incrementAndGet());
		CUSTOM_CREDIT_LIMIT_CHECKS.add (withId);

		List<TestReferenceOrder> enumList = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == referenceOrderId)
			.collect(Collectors.toList());
		
		Set<Long> newCreditCheckIds = new HashSet<>(referenceOrder.creditChecksIds());
		newCreditCheckIds.add(withId.id());
		
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		ReferenceOrderTo updatedReferenceOrder = ImmutableReferenceOrderTo.copyOf(referenceOrder)
				.withCreditChecksIds(newCreditCheckIds)
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestReferenceOrder order = enumList.get(0);
	    	order.setEntity (updatedReferenceOrder);
		} else {
			MockOrderController.CUSTOM_ORDERS.remove(referenceOrder);			
			MockOrderController.CUSTOM_ORDERS.add(updatedReferenceOrder);
		}
		return withId.id();
    }
    
    @ApiOperation("Update of the Credit Check for a Reference Order")
    public void updateReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1") @PathVariable long creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck) {
		Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
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
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "1") @PathVariable long referenceOrderId) {
    	Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
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
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1") @PathVariable long creditCheck) {
    	Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
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
}
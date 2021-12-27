package com.matthey.pmm.toms.service.impl;


import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsCreditCheckService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.transport.CreditCheckTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Transactional
public abstract class CreditCheckServiceImpl implements TomsCreditCheckService {
	@Autowired
	protected TomsValidator validator;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;

	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;
	
	@Autowired
	protected CreditCheckRepository creditCheckRepo;
	
	@Autowired
	protected CreditCheckConverter creditCheckConverter;
	    
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
    public Set<CreditCheckTo> getCreditCheckLimitOrders (
    		@ApiParam(value = "The order ID of the order the Credit Check object is to be retrieved from", example = "100001") @PathVariable long limitOrderId) {
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
    		@ApiParam(value = "The order ID of the limit order the Credit Check is to be retrieved from", example = "100001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to retrieve ", example = "1000000") @PathVariable long creditCheckId) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "getCreditCheckLimitOrder", "limitOrderId", false);
    	
    	if (limitOrder.get().getCreditChecks() == null || limitOrder.get().getCreditChecks().size() == 0) {
    		return null;
    	}
    	
    	Optional<CreditCheck> creditCheck =  validator.verifyCreditCheck (limitOrder.get(), creditCheckId, getClass(), "getCreditCheckLimitOrder", "creditCheckId", false);
    	
       	return creditCheckConverter.toTo(creditCheck.get()); 
    }
    
    @ApiOperation("Creation of a new Credit Check for a Limit Order")
    public long postLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "100001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new Credit Check. ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "postLimitOrderCreditCheck", "limitOrderId", false);

    	// validation checks
    	validator.validateCreditCheckFields(this.getClass(), "postLimitOrderCreditCheck", "newCreditCheck", newCreditCheck, true, null);

    	CreditCheck persisted = creditCheckConverter.toManagedEntity(newCreditCheck);		
		limitOrder.get().getCreditChecks().add(persisted);
		limitOrder.get().setLastUpdate(new Date());
		limitOrder.get().setVersion(limitOrder.get().getVersion()+1);
		limitOrderRepo.save(new LimitOrder(limitOrder.get()));
		return persisted.getId();    	
    }

    @ApiOperation("Update of the Credit Check for a Limit Order")
    public void updateLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "100001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000000") @PathVariable long creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck) {

    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "updateLimitOrderCreditCheck", "limitOrderId", false);
    	Optional<CreditCheck> oldCreditCheck = validator.verifyCreditCheck(limitOrder.get(), creditCheckId, getClass(),"updateLimitOrderCreditCheck", "creditCheckId", false);
    	
    	validator.validateCreditCheckFields(getClass(), "updateLimitOrderCreditCheck", "limitOrderId", existingCreditCheck, false,  creditCheckConverter.toTo(oldCreditCheck.get()));
    	CreditCheck needsToBePersisted = creditCheckConverter.toManagedEntity(existingCreditCheck);
    	creditCheckRepo.save(needsToBePersisted);
		limitOrder.get().setLastUpdate(new Date());
		limitOrder.get().setVersion(limitOrder.get().getVersion()+1);
		limitOrderRepo.save(new LimitOrder(limitOrder.get()));    	
    }
    
    @ApiOperation("Creation of a new Credit Check for a Reference Order")
    public long postReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new Credit Check . ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "postReferenceOrderCreditCheck", "referenceOrderId", false);

    	// validation checks
    	validator.validateCreditCheckFields(this.getClass(), "postReferenceOrderCreditCheck", "newCreditCheck", newCreditCheck, true, null);

    	CreditCheck persisted = creditCheckConverter.toManagedEntity(newCreditCheck);		
    	referenceOrder.get().getCreditChecks().add(persisted);
    	referenceOrder.get().setLastUpdate(new Date());
    	referenceOrder.get().setVersion(referenceOrder.get().getVersion()+1);
		referenceOrderRepo.save(new ReferenceOrder (referenceOrder.get()));
		return persisted.getId();
    }
    
    @ApiOperation("Update of the Credit Check for a Reference Order")
    public void updateReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000007") @PathVariable long creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "updateReferenceOrderCreditCheck", "referenceOrderId", false);
    	Optional<CreditCheck> oldCreditCheck = validator.verifyCreditCheck(referenceOrder.get(), creditCheckId, getClass(),"updateReferenceOrderCreditCheck", "creditCheckId", false);
    	
    	validator.validateCreditCheckFields(getClass(), "updateReferenceOrderCreditCheck", "referenceOrderId", existingCreditCheck, false,  creditCheckConverter.toTo(oldCreditCheck.get()));
    	CreditCheck needsToBePersisted = creditCheckConverter.toManagedEntity(existingCreditCheck);
    	creditCheckRepo.save(needsToBePersisted);
    	referenceOrder.get().setLastUpdate(new Date());
    	referenceOrder.get().setVersion(referenceOrder.get().getVersion()+1);
		referenceOrderRepo.save(new ReferenceOrder(referenceOrder.get()));
    }

    
    @ApiOperation("Retrieval of the Credit Check data for a Reference Order")
    public Set<CreditCheckTo> getCreditChecksReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "100003") @PathVariable long referenceOrderId) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "getCreditChecksReferenceOrders", "referenceOrderId", false);
    	
    	if (referenceOrder.get().getCreditChecks() == null || referenceOrder.get().getCreditChecks().size() == 0) {
    		return null;
    	}
	
    	Set<CreditCheckTo> creditChecks = referenceOrder.get().getCreditChecks().stream()
    			.map(x -> creditCheckConverter.toTo(x))
    			.collect(Collectors.toSet());
    	return creditChecks; 
    }
    
    @ApiOperation("Retrieval of the Credit Check data for a Reference Order")
    public CreditCheckTo getCreditChecksReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000007") @PathVariable long creditCheckId) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "getCreditChecksReferenceOrder", "referenceOrderId", false);
    	
    	if (referenceOrder.get().getCreditChecks() == null || referenceOrder.get().getCreditChecks().size() == 0) {
    		return null;
    	}
    	
    	Optional<CreditCheck> creditCheck =  validator.verifyCreditCheck (referenceOrder.get(), creditCheckId, getClass(), "getCreditChecksReferenceOrder", "creditCheckId", false);
    	
       	return creditCheckConverter.toTo(creditCheck.get()); 
    }
}
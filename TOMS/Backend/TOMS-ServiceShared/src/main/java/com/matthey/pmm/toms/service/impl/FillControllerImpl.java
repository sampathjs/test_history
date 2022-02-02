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

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsFillService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.OrderStatusConverter;
import com.matthey.pmm.toms.transport.FillTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Transactional
public abstract class FillControllerImpl implements TomsFillService {	
	@Autowired 
	protected TomsValidator validator;
	
	@Autowired
	protected OrderStatusConverter orderStatusConverter;
	
	@Autowired 
	protected FillRepository fillRepo;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;

	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;
	
	@Autowired
	protected FillConverter fillConverter;
		
    @ApiOperation("Retrieval of a single fill for a Limit Order")
    public FillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100000") @PathVariable long limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1000001") @PathVariable long fillId) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "getLimitOrderFill", "limitOrderId", false);
    	
    	if (limitOrder.get().getFills() == null || limitOrder.get().getFills().size() == 0) {
    		return null;
    	}
    	
    	Optional<Fill> fill =  validator.verifyFill (limitOrder.get(), fillId, getClass(), "getLimitOrderFill", "fillId", false);    	
       	return fillConverter.toTo(fill.get()); 
    }
    
    @ApiOperation("Retrieval of all fills for a Limit Order")
    public Set<FillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100000") @PathVariable long limitOrderId) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "getLimitOrderFills", "limitOrderId", false);
    	
    	if (limitOrder.get().getFills() == null || limitOrder.get().getFills().size() == 0) {
    		return null;
    	}
	
    	Set<FillTo> fills = limitOrder.get().getFills().stream()
    			.map(x -> fillConverter.toTo(x))
    			.collect(Collectors.toSet());
    	return fills;
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    public long postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be 0. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "postLimitOrderFill", "limitOrderId", false);

    	// validation checks
    	validator.validateFillFields(this.getClass(), "postLimitOrderFill", "newOrderFill", newOrderFill, limitOrder.get(), true, null);

    	Fill persisted = fillConverter.toManagedEntity(newOrderFill);		
		limitOrder.get().getFills().add(persisted);
		limitOrder.get().setLastUpdate(new Date());
		limitOrder.get().onPreUpdate();
   		if (limitOrder.get().getFillPercentage() >= 0.999999) {
    		limitOrder.get().setOrderStatus(orderStatusConverter.toManagedEntity(DefaultOrderStatus.LIMIT_ORDER_FILLED.getEntity())); 
   		} else if (limitOrder.get().getFillPercentage() >= 0.00d) {
    		limitOrder.get().setOrderStatus(orderStatusConverter.toManagedEntity(DefaultOrderStatus.LIMIT_ORDER_PART_FILLED.getEntity()));    			
   		}
   		limitOrder.get().setVersion(limitOrder.get().getVersion()+1);
		limitOrderRepo.save(limitOrder.get());		

		return persisted.getId();
    }
    
    @ApiOperation("Update of an existing fill for a Limit Order")
    public long updateLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the fill object to be updated", example = "1") @PathVariable long fillId,
    		@ApiParam(value = "The new fill. ID has to be fillId.", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "updateLimitOrderFill", "limitOrderId", false);
    	Optional<Fill> existingFill = validator.verifyFill(limitOrder.get(), fillId, getClass(), "updateReferenceOrderFill", "fillId", false);

    	// validation checks
    	validator.validateFillFields(this.getClass(), "updateLimitOrderFill", "newOrderFill", newOrderFill, limitOrder.get(), false, fillConverter.toTo(existingFill.get()));

    	Fill persisted = fillConverter.toManagedEntity(newOrderFill);
		limitOrder.get().setLastUpdate(new Date());
		limitOrder.get().onPreUpdate();
   		if (limitOrder.get().getFillPercentage() >= 0.999999) {
    		limitOrder.get().setOrderStatus(orderStatusConverter.toManagedEntity(DefaultOrderStatus.LIMIT_ORDER_FILLED.getEntity())); 
   		} else if (limitOrder.get().getFillPercentage() >= 0.00d) {
    		limitOrder.get().setOrderStatus(orderStatusConverter.toManagedEntity(DefaultOrderStatus.LIMIT_ORDER_PART_FILLED.getEntity()));    			
   		}
   		limitOrder.get().setVersion(limitOrder.get().getVersion()+1);
		limitOrderRepo.save(limitOrder.get());		

		return persisted.getId();
    }
    	
    @ApiOperation("Retrieval of a the fill for a Reference Order, if present")
    public Set<FillTo> getReferenceOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100003") @PathVariable long referenceOrderId) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "getReferenceOrderFills", "referenceOrderId", false);
    	
    	if (referenceOrder.get().getFills() == null || referenceOrder.get().getFills().size() == 0) {
    		return null;
    	}
	
    	Set<FillTo> fills = referenceOrder.get().getFills().stream()
    			.map(x -> fillConverter.toTo(x))
    			.collect(Collectors.toSet());
    	return fills;
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    public long postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be 0. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "postReferenceOrderFill", "referenceOrderId", false);

    	// validation checks
    	validator.validateFillFields(this.getClass(), "postReferenceOrderFill", "newOrderFill", newOrderFill, referenceOrder.get(), true, null);

    	Fill persisted = fillConverter.toManagedEntity(newOrderFill);		
		referenceOrder.get().getFills().add(persisted);
		referenceOrder.get().setLastUpdate(new Date());
		referenceOrder.get().onPreUpdate();
		referenceOrder.get().setVersion(referenceOrder.get().getVersion()+1);
		referenceOrderRepo.save(referenceOrder.get());
		return persisted.getId();
    }
    
    @ApiOperation("Retrieval of a the fill for a Reference Order, if present")
    public FillTo getReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1000002") @PathVariable long fillId) {
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "getReferenceOrderFill", "referenceOrderId", false);
    	
    	if (referenceOrder.get().getFills() == null || referenceOrder.get().getFills().size() == 0) {
    		return null;
    	}
    	
    	Optional<Fill> fill =  validator.verifyFill (referenceOrder.get(), fillId, getClass(), "getReferenceOrderFill", "fillId", false);    	
       	return fillConverter.toTo(fill.get());  	
    }
    
    @ApiOperation("Update of an existing fill for a Reference Order")
    public long updateReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the fill object to be updated", example = "1") @PathVariable long fillId,    		
    		@ApiParam(value = "The new fill. ID has to be matching fillId.", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) { 
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "updateReferenceOrderFill", "referenceOrderId", false);
    	Optional<Fill> existingFill = validator.verifyFill(referenceOrder.get(), fillId, getClass(), "updateReferenceOrderFill", "fillId", false);
    	    	
    	// validation checks
    	validator.validateFillFields(this.getClass(), "updateReferenceOrderFill", "newOrderFill", newOrderFill, referenceOrder.get(), false, fillConverter.toTo(existingFill.get()));

    	Fill updatedEntity = fillConverter.toManagedEntity(newOrderFill);
    	fillRepo.save(updatedEntity);
		referenceOrder.get().setLastUpdate(new Date());
		referenceOrder.get().onPreUpdate();
		referenceOrder.get().setVersion(referenceOrder.get().getVersion()+1);
		referenceOrderRepo.save(referenceOrder.get());
		return updatedEntity.getId();
    }


}
package com.matthey.pmm.toms.service.impl;


import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsOrderService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderLegConverter;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public abstract class OrderControllerImpl implements TomsOrderService {
	@Autowired
	protected Validator validator;
	
	@Autowired 
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;

	@Autowired
	protected ReferenceOrderLegRepository referenceOrderLegRepo;
	
	@Autowired
	protected LimitOrderConverter limitOrderConverter;
	
	@Autowired
	protected ReferenceOrderConverter referenceOrderConverter;	

	@Autowired
	protected ReferenceOrderLegConverter referenceOrderLegConverter;	
	
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
	public long postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID and version have to be 0. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder) {
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
    	Optional<LimitOrder> oldLimitOrderManaged = limitOrderRepo.findLatestByOrderId(existingLimitOrder.id());
    	
    	if (oldLimitOrderManaged.isEmpty()) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrder", "existingLimitOrder.id" , "Limit Order", "" + existingLimitOrder.id());
    	}
    	validator.validateLimitOrderFields (this.getClass(), "updateLimitOrder", "existingLimitOrder", existingLimitOrder, false, 
    			limitOrderConverter.toTo(oldLimitOrderManaged.get()));

    	LimitOrder existingLimitOrderManaged = limitOrderConverter.toManagedEntity(existingLimitOrder);
    	existingLimitOrderManaged.setVersion(existingLimitOrderManaged.getVersion()+1);
    	limitOrderRepo.save(existingLimitOrderManaged);
    }
	
	@Override
    @ApiOperation("Retrieval of Reference Order Data")
	public Set<OrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20006", required = false) @RequestParam(required=false) Long internalBuId,
			@ApiParam(value = "The external party IDs the Reference orders are supposed to be retrieved for. Null or 0 = all orders", example = "20022", required = false) @RequestParam(required=false) Long externalBuId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Long buySellId,
			@ApiParam(value = "Version ID, -1 = all limit orders", example = "1", required = false) @RequestParam(required=false) Integer versionId) {
		Optional<Party> intBu = validator.verifyParty(internalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT), getClass(), "getReferenceOrders", "internalBuId", true);
		Optional<Party> extBu = validator.verifyParty(externalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT), getClass(), "getReferenceOrders", "externalBuId", true);
		Date minCreatedAt = validator.verifyDateTime (minCreatedAtDate, getClass(), "getReferenceOrders", "minCreatedAtDate");
		Date maxCreatedAt = validator.verifyDateTime (maxCreatedAtDate, getClass(), "getReferenceOrders", "maxCreatedAtDate");
		Optional<Reference> buySell = validator.verifyDefaultReference(buySellId, Arrays.asList(DefaultReferenceType.BUY_SELL), getClass(), "getReferenceOrders", "buySellId", true); 
		
		List<ReferenceOrder> matchingOrders = referenceOrderRepo.findByOrderIdAndOptionalParameters(intBu.orElse(null), extBu.orElse(null), buySell.orElse(null), minCreatedAt, maxCreatedAt, versionId);
		return matchingOrders.stream()
				.map(x -> referenceOrderConverter.toTo(x))
				.collect(Collectors.toSet());
	}
    
    @ApiOperation("Creation of a new Reference Order")
	public long postReferenceOrder (@ApiParam(value = "The new Reference Order. Order ID and version have to be 0. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder) {
    	// validation checks
    	validator.validateReferenceOrderFields (this.getClass(), "postReferenceOrder", "newReferenceOrder", newReferenceOrder, true, null);
	
		ReferenceOrder managedEntity = referenceOrderConverter.toManagedEntity(newReferenceOrder);
		managedEntity.setCreatedAt(new Date());
		managedEntity = referenceOrderRepo.save(managedEntity);
    	return managedEntity.getOrderId();
    }
    
    @ApiOperation("Update of an existing Reference Order")
	public void updateReferenceOrder (@ApiParam(value = "The Reference Order to update. Order ID has to denote an existing Reference Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder) {
    	Optional<ReferenceOrder> oldOrderManaged = referenceOrderRepo.findLatestByOrderId(existingReferenceOrder.id());
    	
    	if (oldOrderManaged.isEmpty()) {
    		throw new UnknownEntityException (this.getClass(), "updateReferenceOrder", "existingReferenceOrder.id" , "Referencet Order", "" + existingReferenceOrder.id());
    	}
    	validator.validateReferenceOrderFields (this.getClass(), "updateReferenceOrder", "existingReferenceOrder", existingReferenceOrder, false, 
    			referenceOrderConverter.toTo(oldOrderManaged.get()));

    	ReferenceOrder existingOrderManaged = referenceOrderConverter.toManagedEntity(existingReferenceOrder);
    	existingOrderManaged.setVersion(existingOrderManaged.getVersion()+1);
    	referenceOrderRepo.save(existingOrderManaged);
    }
    
    @Cacheable({"ReferenceOrderLeg"})
    @ApiOperation("Retrieval of all reference order legs for a single reference order.")
	@GetMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs")
	public Set<ReferenceOrderLegTo> getReferenceOrderLegs (@ApiParam(value = "The ID of the reference order", example = "1000001", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order", example = "1", required = true) @PathVariable(required=true) int version) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "getReferenceOrderLegs", "referenceOrderId", false);
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "getReferenceOrderLegs", "version");
    	return orderManaged.get().getLegs().stream()
    		.map(x -> referenceOrderLegConverter.toTo(x))
    		.collect(Collectors.toSet());
    }
    
    @ApiOperation("Creation of a new Reference Order Leg")
	@PostMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs")
	public long postReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "1000001", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The new Reference Order Leg, ID has to be -1. The actual assigned ID is going to be returned.", example = "", required = true) @RequestBody(required=true) ReferenceOrderLegTo newReferenceOrderLeg) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "postReferenceOrderLeg", "referenceOrderId", false);
    	if (orderManaged.get().getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
    		throw new IllegalStateException(getClass(), "postReferenceOrderLeg", "referenceOrderId", 
    				orderManaged.get().getOrderStatus().toString(), "ReferenceOrder", DefaultOrderStatus.REFERENCE_ORDER_PENDING.name());
    	}
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "postReferenceOrderLeg", "version");
    	validator.validateReferenceOrderLegFields (this.getClass(), "postReferenceOrderLeg", "newReferenceOrderLeg", orderManaged.get(), newReferenceOrderLeg,  false, null);
    	ReferenceOrderLeg newLegManaged = referenceOrderLegConverter.toManagedEntity(newReferenceOrderLeg);
    	orderManaged.get().getLegs().add(newLegManaged);
    	referenceOrderRepo.save(orderManaged.get());
    	return newLegManaged.getId();
    }

    @ApiOperation("Deletion of an existing Reference Order Leg")
	@DeleteMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void deleteReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "1000001", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000001", required = true) @PathVariable(required=true) int legId) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "deleteReferenceOrderLeg", "referenceOrderId", false);
    	if (orderManaged.get().getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
    		throw new IllegalStateException(getClass(), "deleteReferenceOrderLeg", "referenceOrderId", 
    				orderManaged.get().getOrderStatus().toString(), "ReferenceOrder", DefaultOrderStatus.REFERENCE_ORDER_PENDING.name());
    	}
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "deleteReferenceOrderLeg", "version");
    	orderManaged.get().getLegs().removeIf(x -> x.getId() == legId);
    	referenceOrderRepo.save(orderManaged.get());
    	// remove leg from association with order/orderversion, but keep leg entity.
    	// reason: leg might be used in an older version of the order as well.
    }
    
    @ApiOperation("Update of an existing Reference Order Leg")
    @PutMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void updateReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "100005", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version.", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000001", required = true) @PathVariable(required=true) int legId,
			@ApiParam(value = "The new Reference Order Leg, ID has to be matching legId.", example = "", required = true) @RequestBody(required=true) ReferenceOrderLegTo existingReferenceOrderLeg) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "updateReferenceOrderLeg", "referenceOrderId", false);
    	if (orderManaged.get().getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
    		throw new IllegalStateException(getClass(), "updateReferenceOrderLeg", "referenceOrderId", 
    				orderManaged.get().getOrderStatus().toString(), "ReferenceOrder", DefaultOrderStatus.REFERENCE_ORDER_PENDING.name());
    	}
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "updateReferenceOrderLeg", "version");
    	Optional<ReferenceOrderLeg> existingLeg = validator.verifyReferenceOrderLegId(legId, this.getClass(), "postReferenceOrderLeg", "legId", false);
    	validator.validateReferenceOrderLegFields (this.getClass(), "postReferenceOrderLeg", "newReferenceOrderLeg", orderManaged.get(), existingReferenceOrderLeg,  true, existingLeg.get());
    	ReferenceOrderLeg updatedLeg = referenceOrderLegConverter.toManagedEntity(existingReferenceOrderLeg);
    	referenceOrderLegRepo.save(updatedLeg);
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
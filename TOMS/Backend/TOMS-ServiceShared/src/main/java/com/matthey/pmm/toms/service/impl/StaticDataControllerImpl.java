package com.matthey.pmm.toms.service.impl;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.ExpirationStatusRepository;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.service.TomsStaticDataService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.conversion.OrderStatusConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceTypeConverter;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public abstract class StaticDataControllerImpl implements TomsStaticDataService {
	@Autowired 
	protected TomsValidator validator;
	
	@Autowired
	protected ReferenceTypeRepository refTypeRepo;

	@Autowired
	protected ReferenceRepository refRepo;

	@Autowired
	protected OrderStatusRepository orderStatusRepo;

	@Autowired
	protected ExpirationStatusRepository expirationStatusRepo;
	
	@Autowired 
	protected ReferenceConverter refConverter;

	@Autowired 
	protected ReferenceTypeConverter refTypeConverter;
	
	@Autowired
	protected OrderStatusConverter orderStatusConverter;
		
	
	@Override
	public Set<ReferenceTypeTo> getAllReferenceTypes () {
		return StreamSupport.stream(refTypeRepo.findAll().spliterator(), false).map(x -> refTypeConverter.toTo(x)).collect(Collectors.toSet());
	}
	
	@Override
	@ApiOperation("Retrieval of References")
	public Set<ReferenceTo> getReferences (
			@ApiParam(value = "Reference Type ID, 0 or null = all", example = "1", required = false) @RequestParam(required=false) Long referenceTypeId) {
		if (referenceTypeId != null && referenceTypeId !=  0) {
			return refRepo.findByTypeId(referenceTypeId).stream().map(x -> refConverter.toTo(x)).collect(Collectors.toSet());
		} else {
			return StreamSupport.stream(refRepo.findAll().spliterator(), false).map(x -> refConverter.toTo(x)).collect(Collectors.toSet());
		}
	}
	
	@Override
    @ApiOperation("Retrieval of Order Status (dependent on order type)")
	public Set<OrderStatusTo> getOrderStatus (
			@ApiParam(value = "Order Status ID, 0 or null for all", example = "7", required = false) @RequestParam(required=false) Long orderStatusId,
			@ApiParam(value = "Order Type Name ID, 0 or null for all", example = "13", required = false) @RequestParam(required=false) Long orderTypeNameId) {
		Optional<Reference> orderStatusName = validator.verifyDefaultReference(orderStatusId, 
				Arrays.asList(DefaultReferenceType.ORDER_STATUS_NAME),  this.getClass(), "getOrderStatus", "orderStatusId", orderStatusId == null || orderStatusId == 0);
		Optional<Reference> orderTypeName = validator.verifyDefaultReference(orderTypeNameId, 
				Arrays.asList(DefaultReferenceType.ORDER_TYPE_NAME),  this.getClass(), "getOrderStatus", "orderTypeNameId", orderTypeNameId == null || orderTypeNameId == 0);
		
    	if (orderStatusName.isPresent()) {
			if (orderTypeName.isPresent()) {
				return orderStatusRepo.findByOrderStatusNameAndOrderType(orderStatusName.get(), orderTypeName.get()).stream()
						.map(x -> orderStatusConverter.toTo(x)).collect(Collectors.toSet());
			} else {
				return orderStatusRepo.findByOrderStatusName(orderStatusName.get()).stream()
						.map(x -> orderStatusConverter.toTo(x)).collect(Collectors.toSet());
			}
		} else {
			if (orderTypeName.isPresent()) {
				return orderStatusRepo.findByOrderType(orderTypeName.get()).stream()
						.map(x -> orderStatusConverter.toTo(x)).collect(Collectors.toSet());
			} else {
				return StreamSupport.stream (orderStatusRepo.findAll().spliterator(), false)
						.map(x -> orderStatusConverter.toTo(x)).collect(Collectors.toSet());
			}
		}
	}
}

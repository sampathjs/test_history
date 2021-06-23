package com.matthey.pmm.toms.service.mock;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.DefaultExpirationStatus;
import com.matthey.pmm.toms.enums.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsStaticDataService;
import com.matthey.pmm.toms.transport.ExpirationStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockStaticDataController implements TomsStaticDataService {

	@Override
	public Set<ReferenceTypeTo> getAllReferenceTypes () {
		return new HashSet<>(DefaultReferenceType.asList());
	}
	
	@Override
	@ApiOperation("Retrieval of References")
	public Set<ReferenceTo> getReferences (
			@ApiParam(value = "Reference Type ID, 0 or null = all", example = "1", required = false) @RequestParam(required=false) Integer referenceTypeId) {
		if (referenceTypeId != null && referenceTypeId !=  0) {
			return new HashSet<>(DefaultReference.asList().stream().filter(x -> x.idType() == referenceTypeId).collect(Collectors.toList()));			
		} else {
			return new HashSet<>(DefaultReference.asList());
		}
	}
	
	@Override
    @ApiOperation("Retrieval of Order Status (dependent on order type)")
	public Set<OrderStatusTo> getOrderStatus (
			@ApiParam(value = "Order Status ID, 0 or null for all", example = "7", required = false) @RequestParam(required=false) Integer orderStatusId,
			@ApiParam(value = "Order Type Name ID, 0 or null for all", example = "13", required = false) @RequestParam(required=false) Integer orderTypeNameId) {
		if (orderStatusId != null && orderStatusId !=  0) {
			if (orderTypeNameId != null && orderTypeNameId != 0) {
				return new HashSet<>(DefaultOrderStatus.asList().stream().filter(x -> x.idOrderTypeName() == orderTypeNameId && x.id() == orderStatusId).collect(Collectors.toList()));				
			} else {
				return new HashSet<>(DefaultOrderStatus.asList().stream().filter(x -> x.id() == orderStatusId).collect(Collectors.toList()));								
			}
		} else {
			if (orderTypeNameId != null && orderTypeNameId != 0) {
				return new HashSet<>(DefaultOrderStatus.asList().stream().filter(x -> x.idOrderTypeName() == orderTypeNameId).collect(Collectors.toList()));
			} else {
				return new HashSet<>(DefaultOrderStatus.asList());				
			}
		}		
	}
	    
    @ApiOperation("Retrieval of Expiration Status (dependent on order type)")
	public Set<ExpirationStatusTo> getExpirationStatus (
			@ApiParam(value = "Expiration Status ID, 0 or null for all", example = "1", required = false) @RequestParam(required=false) Integer expirationStatusId,
			@ApiParam(value = "Order Type Name ID, 0 or null for all", example = "17", required = false) @RequestParam(required=false) Integer orderTypeNameId) {
		if (expirationStatusId != null && expirationStatusId !=  0) {
			if (orderTypeNameId != null && orderTypeNameId != 0) {
				return new HashSet<>(DefaultExpirationStatus.asList().stream().filter(x -> x.idOrderTypeName() == orderTypeNameId && x.id() == expirationStatusId).collect(Collectors.toList()));				
			} else {
				return new HashSet<>(DefaultExpirationStatus.asList().stream().filter(x -> x.id() == expirationStatusId).collect(Collectors.toList()));								
			}
		} else {
			if (orderTypeNameId != null && orderTypeNameId != 0) {
				return new HashSet<>(DefaultExpirationStatus.asList().stream().filter(x -> x.idOrderTypeName() == orderTypeNameId).collect(Collectors.toList()));
			} else {
				return new HashSet<>(DefaultExpirationStatus.asList());				
			}
		}    	
    }
}

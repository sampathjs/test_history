package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.ExpirationStatusTo;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Static Data"}, description = "APIs for relevant static data")
@RequestMapping(API_PREFIX)
public interface TomsStaticDataService {

    @Cacheable({"ReferenceTypes"})
    @ApiOperation("Retrieval of all Reference Types")
	@GetMapping("/referenceTypes")
	public Set<ReferenceTypeTo> getAllReferenceTypes ();

    @Cacheable({"References"})
    @ApiOperation("Retrieval of References")
	@GetMapping("/references")
	public Set<ReferenceTo> getReferences (
			@ApiParam(value = "Reference Type ID, 0 or null = all", example = "1", required = false) @RequestParam(required=false) Long referenceTypeId);

    @Cacheable({"OrderStatus"})
    @ApiOperation("Retrieval of Order Status (dependent on order type)")
	@GetMapping("/orderStatus")
	public Set<OrderStatusTo> getOrderStatus (
			@ApiParam(value = "Order Status ID, 0 or null for all", example = "7", required = false) @RequestParam(required=false) Long orderStatusId,
			@ApiParam(value = "Order Type Name ID, 0 or null for all", example = "13", required = false) @RequestParam(required=false) Long orderTypeNameId);
    
    @Cacheable({"ExpirationStatus"})
    @ApiOperation("Retrieval of Expiration Status (dependent on order type)")
	@GetMapping("/expirationStatus")
	public Set<ExpirationStatusTo> getExpirationStatus (
			@ApiParam(value = "Expiration Status ID, 0 or null for all", example = "1", required = false) @RequestParam(required=false) Long expirationStatusId,
			@ApiParam(value = "Order Type Name ID, 0 or null for all", example = "17", required = false) @RequestParam(required=false) Long orderTypeNameId);

}

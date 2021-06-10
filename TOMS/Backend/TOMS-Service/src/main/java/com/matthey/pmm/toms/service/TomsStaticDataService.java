package com.matthey.pmm.toms.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.cache.annotation.Cacheable;


import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.BuySell;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

@Api(tags = {"Static Data"}, description = "APIs for relevant static data")
@RequestMapping(API_PREFIX)
public interface TomsStaticDataService {

    @Cacheable({"ReferenceTypes"})
    @ApiOperation("Retrieval of all Reference Types")
	@GetMapping("/referenceTypes")
	public Set<ReferenceType> getAllReferenceTypes ();

    @Cacheable({"References"})
    @ApiOperation("Retrieval of References")
	@GetMapping("/references")
	public Set<Reference> getReferences (
			@ApiParam(value = "Reference Type ID, 0 or null = all", example = "1", required = false) @RequestParam(required=false) Integer referenceTypeId);

    @Cacheable({"OrderStatus"})
    @ApiOperation("Retrieval of Order Status (dependent on order type)")
	@GetMapping("/orderStatus")
	public Set<OrderStatus> getOrderStatus (
			@ApiParam(value = "Order Status ID, 0 or null for all", example = "7", required = false) @RequestParam(required=false) Integer orderStatusId,
			@ApiParam(value = "Order Type Name ID, 0 or null for all", example = "13", required = false) @RequestParam(required=false) Integer orderTypeNameId);

    @Cacheable({"BuySell"})
    @ApiOperation("Retrieval of Buy Sell")
	@GetMapping("/buySell")
	public Set<BuySell> getBuySell (
			@ApiParam(value = "BuySell ID, null for all", example = "0", required = false) @RequestParam(required=false) Integer buySellId);
}

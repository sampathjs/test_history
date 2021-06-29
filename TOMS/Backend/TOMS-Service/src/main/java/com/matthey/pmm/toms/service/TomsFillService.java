package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Fill services for Limit and Reference Orders"}, description = "API for order fills")
@RequestMapping(API_PREFIX)
public interface TomsFillService {
    @Cacheable({"LimitOrderFill"})
    @ApiOperation("Retrieval of a single fill for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/fill/{fillId}")    
    public FillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable int fillId);
    
    @Cacheable({"LimitOrderFill"})
    @ApiOperation("Retrieval of a all fills for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/fill")
    public Set<FillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId);

    @ApiOperation("Creation of a new fills for a Limit Order")
	@PostMapping("/limitOrder/{limitOrderId}/fill")    
    public int postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill);
    

    @Cacheable({"ReferenceOrderFill"})
    @ApiOperation("Retrieval of a the fill for a Reference Order, if present")
	@GetMapping("/referenceOrder/{referenceOrderId}/fill")    
    public FillTo getReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId);
    
    @ApiOperation("Creation of a new fill for a Reference Order")
	@PostMapping("/referenceOrder/{referenceOrderId}/fill")
    public int postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill);    
    
}

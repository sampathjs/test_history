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

import com.matthey.pmm.toms.transport.FillTo;

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
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable long fillId);
    
    @Cacheable({"LimitOrderFill"})
    @ApiOperation("Retrieval of a all fills for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/fill")
    public Set<FillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long limitOrderId);

    @ApiOperation("Creation of a new fills for a Limit Order")
	@PostMapping("/limitOrder/{limitOrderId}/fill")    
    public long postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill);

    @ApiOperation("Update of an existing fill for a Limit Order")
    @PutMapping("/limitOrder/{limitOrderId}/fill/{fillId}")    
    public long updateLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the fill object to be updated", example = "1") @PathVariable long fillId,
    		@ApiParam(value = "The new fill. ID has to be fillId.", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill);
    

    @Cacheable({"ReferenceOrderFill"})
    @ApiOperation("Retrieval of a the fill for a Reference Order, if present")
	@GetMapping("/referenceOrder/{referenceOrderId}/fill/{fillId}")    
    public FillTo getReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable long fillId);
    
    
    @Cacheable({"ReferenceOrderFill"})
    @ApiOperation("Retrieval of a all fills for a Limit Order")
	@GetMapping("/referenceOrder/{referenceOrderId}/fill")
    public Set<FillTo> getReferenceOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long referenceOrderId);

    
    @ApiOperation("Creation of a new fill for a Reference Order")
	@PostMapping("/referenceOrder/{referenceOrderId}/fill")
    public long postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill);    

    @ApiOperation("Update of an existing fill for a Reference Order")
    @PutMapping("/referenceOrder/{referenceOrderId}/fill/{fillId}")
    public long updateReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the fill object to be updated", example = "1") @PathVariable long fillId,    		
    		@ApiParam(value = "The new fill. ID has to be matching fillId.", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill);    
}

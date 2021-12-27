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

import com.matthey.pmm.toms.transport.CreditCheckTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Credit Check Related Data"}, description = "APIs for Credit Checks")
@RequestMapping(API_PREFIX)
public interface TomsCreditCheckService {    
    @Cacheable({"CreditCheckLimitOrder"})
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/creditChecks/")
    public Set<CreditCheckTo> getCreditCheckLimitOrders (
    		@ApiParam(value = "The order ID of the limit order the Credit Check is to be retrieved from", example = "1000001") @PathVariable long limitOrderId);

    @Cacheable({"CreditCheckLimitOrder"})
    @ApiOperation("Retrieval of the Credit Check data for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/creditChecks/{creditCheckId}")
    public CreditCheckTo getCreditCheckLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the Credit Check is to be retrieved from", example = "1000001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to retrieve ", example = "1000000") @PathVariable long creditCheckId);

    @ApiOperation("Creation of a new Credit Check for a Reference Order")
	@PostMapping("/limitOrder/{limitOrderId}/creditCheck")
    public long postLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1000001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new Credit Check. ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck);

    @ApiOperation("Update of the Credit Check for a Reference Order")
	@PutMapping("/limitOrder/{limitOrderId}/creditCheck/{creditCheckId}")    
    public void updateLimitOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1000001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000000") @PathVariable long creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck);  
                
    @Cacheable({"CreditCheckReferenceOrder"})
    @ApiOperation("Retrieval of the Credit Check data for a Reference Order")
	@GetMapping("/referenceOrder/{referenceOrderId}/creditChecks/")
    public Set<CreditCheckTo> getCreditChecksReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "1000003") @PathVariable long referenceOrderId);

    @Cacheable({"CreditCheckReferenceOrder"})
    @ApiOperation("Retrieval of the Credit Check data for a Reference Order")
	@GetMapping("/referenceOrder/{referenceOrderId}/creditChecks/{creditCheckId}")
    public CreditCheckTo getCreditChecksReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be retrieved from", example = "1000003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000007") @PathVariable long creditCheck);

    
    @ApiOperation("Creation of a new Credit Check for a Reference Order")
	@PostMapping("/referenceOrder/{referenceOrderId}/creditCheck")    
    public long postReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1000003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new Credit Check . ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck);

    @ApiOperation("Update of the Credit Check for a Reference Order")
	@PutMapping("/referenceOrder/{referenceOrderId}/creditCheck/{creditCheckId}")
    public void updateReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "1000003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the Credit Check to update ", example = "1000007") @PathVariable long creditCheckId,
    		@ApiParam(value = "The updated Credit Check. ID has to be matching the ID of the existing Credit Check.", example = "", required = true) @RequestBody(required=true) CreditCheckTo existingCreditCheck); 
}

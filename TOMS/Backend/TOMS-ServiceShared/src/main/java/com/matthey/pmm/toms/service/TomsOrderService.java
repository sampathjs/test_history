package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Reference and Limit Orders"}, description = "APIs for relevant Order operations")
@RequestMapping(API_PREFIX)
public interface TomsOrderService {
	// limit order
    @Cacheable({"LimitOrder"})
    @ApiOperation("Retrieval of Limit Order Data")
	@GetMapping("/limitOrder")
	public Set<OrderTo> getLimitOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId);
           
    @ApiOperation("Creation of a new Limit Order")
	@PostMapping("/limitOrder")
	public int postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder);
    
    @ApiOperation("Update of an existing Limit Order")
	@PutMapping("/limitOrder")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder);

    // reference order    
    @Cacheable({"ReferenceOrder"})
    @ApiOperation("Retrieval of Reference Order Data")
	@GetMapping("/referenceOrder")
	public Set<OrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId);
        
    @ApiOperation("Creation of a new Reference Order")
	@PostMapping("/referenceOrder")
	public int postReferenceOrder (@ApiParam(value = "The new Reference Order, ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder);
    
    @ApiOperation("Update of an existing Reference Order")
	@PutMapping("/referenceOrder")
	public void updateReferenceOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder);
    
    @ApiOperation("Retrieve the value of an attribute of the provided Limit Order based on the other fields of the order")
	@PostMapping("/limitOrder/defaultAndDependentValues")
    public String getAttributeValueLimitOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "idInternalBu", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) LimitOrderTo orderTemplate);
    
    @ApiOperation("Retrieve the value of an attribute of the provided Reference Order based on the other fields of the order")
	@PostMapping("/referenceOrder/defaultAndDependentValues")
    public String getAttributeValueReferenceOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "idInternalBu", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) ReferenceOrderTo orderTemplate);
}

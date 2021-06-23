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
import com.matthey.pmm.toms.transport.OrderCreditCheckTo;
import com.matthey.pmm.toms.transport.OrderFillTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Order Data"}, description = "APIs for relevant Order operations")
@RequestMapping(API_PREFIX)
public interface TomsOrderService {
	// limit order
    @Cacheable({"LimitOrder"})
    @ApiOperation("Retrieval of Limit Order Data")
	@GetMapping("/limitOrder")
	public Set<LimitOrderTo> getLimitOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId);

    @Cacheable({"LimitOrderFill"})
    @ApiOperation("Retrieval of a single fill for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/orderFill/{orderFillId}")    
    public OrderFillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable int orderFillId);
    
    @Cacheable({"LimitOrderFill"})
    @ApiOperation("Retrieval of a all fills for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/orderFill")    
    public Set<OrderFillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId);

    @Cacheable({"CreditLimitCheckLimitOrder"})
    @ApiOperation("Retrieval of the credit limit check data for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/creditLimitCheck/}")
    public OrderCreditCheckTo getCreditLimitCheckLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the credit limit check is to be retrieved from", example = "1") @PathVariable int limitOrderId);
    
    @ApiOperation("Creation of a new order fills for a Limit Order")
	@PostMapping("/limitOrder/{limitOrderId}/orderFill")    
    public int postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new Order Fill. ID has to be -1. The actual assigned Order Fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderFillTo newOrderFill);
    
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
	public Set<ReferenceOrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20004", required = false) @RequestParam(required=false) Integer internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20014", required = false) @RequestParam(required=false) Integer externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Integer buySellId);

    @Cacheable({"ReferenceOrderFill"})
    @ApiOperation("Retrieval of a the order fill for a Reference Order, if present")
	@GetMapping("/referenceOrder/{referenceOrderId}/orderFill")    
    public OrderFillTo getReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId);
    
    @ApiOperation("Creation of a new order fills for a Limit Order")
	@PostMapping("/referenceOrder/{referenceOrderId}/orderFill")    
    public int postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the order fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new Order Fill. ID has to be -1. The actual assigned Order Fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderFillTo newOrderFill);
    
    @Cacheable({"CreditLimitCheckReferenceOrder"})
    @ApiOperation("Retrieval of the credit limit check data for a Reference Order")
	@GetMapping("/referenceOrder/{referenceOrderId}/creditLimitCheck/}")
    public OrderCreditCheckTo getCreditLimitCheckReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the credit limit check is to be retrieved from", example = "1") @PathVariable int referenceOrderId);

    @ApiOperation("Creation of a new credit limit check for a Reference Order")
	@PostMapping("/referenceOrder/{referenceOrderId}/creditLimitCheck")    
    public int postReferenceOrderCreditLimitCheck (
    		@ApiParam(value = "The order ID of the reference order the credit limit check is to be posted for ", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new Credit Limit Check . ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCreditCheckTo newCreditLimitCheck);

    @ApiOperation("Update of the credit limit check for a Reference Order")
	@PutMapping("/referenceOrder/{referenceOrderId}/creditLimitCheck")    
    public void updateReferenceOrderCreditLimitCheck (
    		@ApiParam(value = "The order ID of the reference order the credit limit check is to be posted for ", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The updated Credit Limit Check. ID has to be matching the ID of the existing Credit Limit Check.", example = "", required = true) @RequestBody(required=true) OrderCreditCheckTo existingCreditLimitCheck);
    
    @ApiOperation("Creation of a new Reference Order")
	@PostMapping("/referenceOrder")
	public int postReferenceOrder (@ApiParam(value = "The new Reference Order, ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder);
    
    @ApiOperation("Update of an existing Reference Order")
	@PutMapping("/referenceOrder")
	public void updateReferenceOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder);
    
    
}

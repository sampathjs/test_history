package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
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
			@ApiParam(value = "The internal BU IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20006", required = false) @RequestParam(required=false) Long internalBuId,
			@ApiParam(value = "The external BU IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20022", required = false) @RequestParam(required=false) Long externalBuId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Long buySellId,
			@ApiParam(value = "Version ID, null = latest order version", example = "1", required = false) @RequestParam(required=false) Integer versionId
			);
           
    @ApiOperation("Creation of a new Limit Order")
	@PostMapping("/limitOrder")
	public long postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned. Version ID has to be 0", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder);
    
    @ApiOperation("Update of an existing Limit Order")
	@PutMapping("/limitOrder")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update. The version ID has to match the latest one for that order.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder);

    // reference order
    @Cacheable({"ReferenceOrder"})
    @ApiOperation("Retrieval of Reference Order Data")
	@GetMapping("/referenceOrder")
	public Set<OrderTo> getReferenceOrders (
			@ApiParam(value = "The internal party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20006", required = false) @RequestParam(required=false) Long internalPartyId,
			@ApiParam(value = "The external party IDs the limit orders are supposed to be retrieved for. Null or 0 = all orders", example = "20022", required = false) @RequestParam(required=false) Long externalPartyId,
			@ApiParam(value = "Min Creation Date, all orders returned have been created after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Buy/Sell ID, Null or 0 = all orders", example = "15", required = false) @RequestParam(required=false) Long buySellId,
			@ApiParam(value = "Version ID, -1 = all limit orders", example = "1", required = false) @RequestParam(required=false) Integer versionId);
        
    @ApiOperation("Creation of a new Reference Order")
	@PostMapping("/referenceOrder")
	public long postReferenceOrder (@ApiParam(value = "The new Reference Order, ID has to be -1. The actual assigned ID is going to be returned. Version ID has to be 0", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder);
    
    @ApiOperation("Update of an existing Reference Order")
	@PutMapping("/referenceOrder")
	public void updateReferenceOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update. The version ID has to match the latest one for that order.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder);

    // reference order legs
    @Cacheable({"ReferenceOrderLeg"})
    @ApiOperation("Retrieval of all reference order legs for a single reference order.")
	@GetMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs")
	public Set<ReferenceOrderLegTo> getReferenceOrderLegs (@ApiParam(value = "The ID of the reference order", example = "100005", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order", example = "1", required = true) @PathVariable(required=true) int version);
    
    @ApiOperation("Creation of a new Reference Order Leg")
	@PostMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs")
	public long postReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "100003", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The new Reference Order Leg, ID has to be -1. The actual assigned ID is going to be returned.", example = "", required = true) @RequestBody(required=true) ReferenceOrderLegTo newReferenceOrderLeg);

    @ApiOperation("Deletion of an existing Reference Order Leg")
	@DeleteMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void deleteReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "100003", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000002", required = true) @PathVariable(required=true) int legId);

    
    @ApiOperation("Update of an existing Reference Order Leg")
    @PutMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void updateReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "100003", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version.", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000002", required = true) @PathVariable(required=true) int legId,
			@ApiParam(value = "The new Reference Order Leg, ID has to be matching legId.", example = "", required = true) @RequestBody(required=true) ReferenceOrderLegTo existingReferenceOrderLeg);
    
    // attribute value calculation
    
    @ApiOperation("Retrieve the value of an attribute of the provided Limit Order based on the other fields of the order")
	@PostMapping("/limitOrder/defaultAndDependentValues")
    public String getAttributeValueLimitOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "idInternalBu", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) LimitOrderTo orderTemplate);
    
    @ApiOperation("Retrieve the value of an attribute of the provided Reference Order based on the other fields of the order")
	@PostMapping("/referenceOrder/defaultAndDependentValues")
    public String getAttributeValueReferenceOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "idInternalBu", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) ReferenceOrderTo orderTemplate);
    
}

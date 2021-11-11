package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.List;
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
			@ApiParam(value = "List of Order IDs or null for all orders, e.g. 100001, 100002", example = "[100001, 100002]", required = false) @RequestParam(required=false) List<Long> orderIds,
			@ApiParam(value = "List of Version IDs, null = latest order version, e.g. 1", example = "1", required = false) @RequestParam(required=false) List<Integer> versionIds,
			@ApiParam(value = "List of the internal BU IDs the orders are supposed to be retrieved for. Null = all orders, example 20006", example = "20006", required = false) @RequestParam(required=false) List<Long> idInternalBu,
			@ApiParam(value = "List of the external BU IDs the orders are supposed to be retrieved for. Null = all orders, example 20022", example = "20022", required = false) @RequestParam(required=false) List<Long> idExternalBu,
			@ApiParam(value = "List of internal LE IDs the orders are supposed to be retrieved for. Null =  all orders, example 20004", example = "20004", required = false) @RequestParam(required=false) List<Long> idInternalLe,
			@ApiParam(value = "List of the external LE IDs the orders are supposed to be retrieved for. Null = all orders, example 20023", example = "20023", required = false) @RequestParam(required=false) List<Long> idExternalLe,
			@ApiParam(value = "List of the internal portfolio IDs the orders are supposed to be retrieved for. Null = all orders, example 118, 119", example = "118, 119", required = false) @RequestParam(required=false) List<Long> idInternalPfolio,
			@ApiParam(value = "List of the external portfolio IDs the orders are supposed to be retrieved for. Null = all orders, example 119", example = "119", required = false) @RequestParam(required=false) List<Long> idExternalPfolio,
			@ApiParam(value = "List of Buy/Sell IDs, Null all orders, example 15", example = "15", required = false) @RequestParam(required=false) List<Long> idBuySell,
			@ApiParam(value = "List of Base Currency IDs, Null all orders, example 34,35,36", example = "34, 35, 36", required = false) @RequestParam(required=false) List<Long> idBaseCurrency,
			@ApiParam(value = "Min Base Quantity, all orders returned have a at least the provided base quantity , Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double minBaseQuantity,
			@ApiParam(value = "Max Base Quantity, all orders returned have a at max the provided base quantity , Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double maxBaseQuantity,
			@ApiParam(value = "List of Base Quantity Unit IDs, null = all orders, example 28, 29", example = "28, 29", required = false) @RequestParam(required=false) List<Long> idBaseQuantityUnit,
			@ApiParam(value = "List of Term Currency IDs, null = all orders, example 42, 43", example = "42, 43", required = false) @RequestParam(required=false) List<Long> idTermCurrency,
			@ApiParam(value = "Reference has to contain this value, null = all orders", example = "gold", required = false) @RequestParam(required=false) String reference,
			@ApiParam(value = "List of Metal Form IDs, null = all orders, example 164, 165", example = "164, 165", required = false) @RequestParam(required=false) List<Long> idMetalForm,
			@ApiParam(value = "List of Metal Location IDs, null = all orders, example 168, 169", example = "168, 169", required = false) @RequestParam(required=false) List<Long> idMetalLocation,
			@ApiParam(value = "List of Order Status IDs, null = all orders, example 1, 2, 3", example = "1, 2, 3", required = false) @RequestParam(required=false) List<Long> idOrderStatus,
			@ApiParam(value = "Min Creation Date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "Min last update date, all orders returned have been updated on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minLastUpdateDate,
			@ApiParam(value = "Max last update date, all orders returned have been updated on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLastUpdateDate,
			// all above: order fields, all below: limit order fields
			@ApiParam(value = "Min Settle Date, all orders returned have been settled on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minSettleDate,
			@ApiParam(value = "Max Settle Date, all orders returned have been settled on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxSettleDate,
			@ApiParam(value = "Min Start Date Concrete, all orders returned have a concrete start date  on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minStartDateConcrete,
			@ApiParam(value = "Max Start Date Concrete, all orders returned have a concrete start date on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxStartDateConcrete,
			@ApiParam(value = "List of IDs of the symbolic start date, null = all orders, example 186, 187", example = "186, 187", required = false) @RequestParam(required=false) List<Long> idStartDateSymbolic,
			@ApiParam(value = "List of IDs of the price type, null = all orders, example 105, 106", example = "105, 106", required = false) @RequestParam(required=false) List<Long> idPriceType,
			@ApiParam(value = "List of IDs of the part fillable flag, null = all orders, example 97, 98", example = "97, 98", required = false) @RequestParam(required=false) List<Long> idYesNoPartFillable,
			@ApiParam(value = "List of IDs of the stop trigger type, null = all orders, example 109, 110", example = "109, 110", required = false) @RequestParam(required=false) List<Long> idStopTriggerType,
			@ApiParam(value = "List of IDs of the currency cross metal, null = all orders, example 34, 35, 36", example = "34, 35, 36", required = false) @RequestParam(required=false) List<Long> idCurrencyCrossMetal,
			@ApiParam(value = "List of IDs of the validation type, null = all orders, example 184, 185", example = "184, 185", required = false) @RequestParam(required=false) List<Long> idValidationType,
			@ApiParam(value = "Min Expiry Date, all orders returned have expired on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minExpiryDate,
			@ApiParam(value = "Max Expiry Date, all orders returned have expired on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxExpiryDate,
			@ApiParam(value = "Min Execution Likelihood, all orders returned have a at least the provided base quantity , Null = no restrictions", example = "0.00", required = false) @RequestParam(required=false) Double minExecutionLikelihood,
			@ApiParam(value = "Max Execution Likelihood, all orders returned have a at max the provided base quantity , Null = no restrictions", example = "1.00", required = false) @RequestParam(required=false) Double maxExecutionLikelihood,
    		@ApiParam(value = "Min limit price, Null = no restrictions", example = "500.00", required = false) @RequestParam(required=false) Double minLimitPrice,
    		@ApiParam(value = "Max limit price, Null = no restrictions", example = "1250.00", required = false) @RequestParam(required=false) Double maxLimitPrice);

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

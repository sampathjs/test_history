package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = {"Reference and Limit Orders"}, description = "APIs for relevant Order operations")
@RequestMapping(path = API_PREFIX, consumes = MediaType.ALL_VALUE)
public interface TomsOrderService {
	
	// all order types
    @Cacheable({"Order"})
    @ApiOperation("Retrieval of All Order Types")
	@GetMapping("/order")
    @ApiImplicitParams ({
        @ApiImplicitParam(name = "page", dataType = "integer", paramType = "query",
                value = "Results page you want to retrieve (0..N)"),
        @ApiImplicitParam(name = "size", dataType = "integer", paramType = "query",
                value = "Number of records per page."),
        @ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query",
                value = "Sorting criteria in the format: property(,asc|desc). " +
                        "Default sort order is ascending. " +
                        "Multiple sort criteria are supported.")
    })
	public Page<OrderTo> getOrders (
			@ApiParam(value = "List of Order Type IDs, e.g. 13, 14", example = "13, 14", required = false) @RequestParam(required=false) List<Long> idOrderType,
			@ApiParam(value = "List of Order IDs or null for all orders, e.g. 100001, 100002", example = "[100001, 100002]", required = false) @RequestParam(required=false) List<Long> orderIds,
			@ApiParam(value = "List of Version IDs, null = latest order version, e.g. 1", example = "1", required = false) @RequestParam(required=false) List<Integer> versionIds,
			@ApiParam(value = "List of the internal BU IDs the orders are supposed to be retrieved for. Null = all orders, example 20006", example = "20006", required = false) @RequestParam(required=false) List<Long> idInternalBu,
			@ApiParam(value = "List of the external BU IDs the orders are supposed to be retrieved for. Null = all orders, example 20022", example = "20022", required = false) @RequestParam(required=false) List<Long> idExternalBu,
			@ApiParam(value = "List of the internal LE IDs the orders are supposed to be retrieved for. Null =  all orders, example 20004", example = "20004", required = false) @RequestParam(required=false) List<Long> idInternalLe,
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
			@ApiParam(value = "List of IDs for users who have created the order, null = all orders, example 20026", example = "1, 2, 3", required = false) @RequestParam(required=false) List<Long> idCreatedByUser,
			@ApiParam(value = "Min creation date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max creation date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "List of IDs for users who have last updated the order, null = all orders, example 20026", example = "20026", required = false) @RequestParam(required=false) List<Long> idUpdatedByUser,
			@ApiParam(value = "Min last update date, all orders returned have been updated on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minLastUpdateDate,
			@ApiParam(value = "Max last update date, all orders returned have been updated on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLastUpdateDate,
			@ApiParam(value = "Min fill percentage, a real number between 0 and 1,  null for no restriction", example = "0.0", required = false) @RequestParam(required=false) Double minFillPercentage,
			@ApiParam(value = "Max fill percentage, a real number between 0 and 1,  null for no restriction", example = "1.0", required = false) @RequestParam(required=false) Double maxFillPercentage,			
			@ApiParam(value = "List of contract types, null = all orders, example 224, 225", example = "224, 225", required = false) @RequestParam(required=false) List<Long> idContractType,
			@ApiParam(value = "List of ticker IDs, null = all orders, example 231, 271", example = "231, 271", required = false) @RequestParam(required=false) List<Long> idTicker,
    		@ApiIgnore("Ignored because swagger ui shows the wrong params, instead they are explained in the implicit params") Pageable pageable,
			@ApiParam(value = "The ID of the order to be the first element according to the provided sort", example = "100003", required = false) @RequestParam(required=false) Long idFirstOrderIncluded    		
			);
	
	// limit order
    @Cacheable({"LimitOrder"})
    @ApiOperation("Retrieval of Limit Order Data")
	@GetMapping("/limitOrder")
    @ApiImplicitParams ({
        @ApiImplicitParam(name = "page", dataType = "integer", paramType = "query",
                value = "Results page you want to retrieve (0..N)"),
        @ApiImplicitParam(name = "size", dataType = "integer", paramType = "query",
                value = "Number of records per page."),
        @ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query",
                value = "Sorting criteria in the format: property(,asc|desc). " +
                        "Default sort order is ascending. " +
                        "Multiple sort criteria are supported.")
    })
	public Page<OrderTo> getLimitOrders (
			@ApiParam(value = "List of Order Type IDs, e.g. 13, 14", example = "13, 14", required = false) @RequestParam(required=false) List<Long> idOrderType,			
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
			@ApiParam(value = "List of IDs for users who have created the order, null = all orders, example 20026", example = "1, 2, 3", required = false) @RequestParam(required=false) List<Long> idCreatedByUser,
			@ApiParam(value = "Min Creation Date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "List of IDs for users who have last updated the order, null = all orders, example 20026", example = "20026", required = false) @RequestParam(required=false) List<Long> idUpdatedByUser,
			@ApiParam(value = "Min last update date, all orders returned have been updated on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minLastUpdateDate,
			@ApiParam(value = "Max last update date, all orders returned have been updated on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLastUpdateDate,
			@ApiParam(value = "Min fill percentage, a real number between 0 and 1,  null for no restriction", example = "0.0", required = false) @RequestParam(required=false) Double minFillPercentage,
			@ApiParam(value = "Max fill percentage, a real number between 0 and 1,  null for no restriction", example = "1.0", required = false) @RequestParam(required=false) Double maxFillPercentage,	
			@ApiParam(value = "List of contract types, null = all orders, example 224, 225", example = "224, 225", required = false) @RequestParam(required=false) List<Long> idContractType,		
			@ApiParam(value = "List of ticker IDs, null = all orders, example 231, 271", example = "231, 271", required = false) @RequestParam(required=false) List<Long> idTicker,			
			// all above: order fields, all below: limit order fields
			@ApiParam(value = "Min Settle Date, all orders returned have been settled on or after that date. Format 'yyyy-MM-dd' (UTC), null for no restriction", example = "2000-10-31", required = false) @RequestParam(required=false) String minSettleDate,
			@ApiParam(value = "Max Settle Date, all orders returned have been settled on or before that date. Format 'yyyy-MM-dd' (UTC), null for no restriction", example = "2030-10-31", required = false) @RequestParam(required=false) String maxSettleDate,
			@ApiParam(value = "Min Start Date Concrete, all orders returned have a concrete start date  on or after that date. Format 'yyyy-MM-dd' (UTC), null for no restriction", example = "2000-10-31", required = false) @RequestParam(required=false) String minStartDateConcrete,
			@ApiParam(value = "Max Start Date Concrete, all orders returned have a concrete start date on or before that date. Format 'yyyy-MM-dd' (UTC), null for no restriction", example = "2030-10-31", required = false) @RequestParam(required=false) String maxStartDateConcrete,
			@ApiParam(value = "List of IDs of the symbolic start date, null = all orders, example 186, 187", example = "186, 187", required = false) @RequestParam(required=false) List<Long> idStartDateSymbolic,
			@ApiParam(value = "List of IDs of the price type, null = all orders, example 105, 106", example = "105, 106", required = false) @RequestParam(required=false) List<Long> idPriceType,
			@ApiParam(value = "List of IDs of the part fillable flag, null = all orders, example 97, 98", example = "97, 98", required = false) @RequestParam(required=false) List<Long> idYesNoPartFillable,
			@ApiParam(value = "List of IDs of the stop trigger type, null = all orders, example 109, 110", example = "109, 110", required = false) @RequestParam(required=false) List<Long> idStopTriggerType,
			@ApiParam(value = "List of IDs of the currency cross metal, null = all orders, example 34, 35, 36", example = "34, 35, 36", required = false) @RequestParam(required=false) List<Long> idCurrencyCrossMetal,
			@ApiParam(value = "List of IDs of the validation type, null = all orders, example 184, 185", example = "184, 185", required = false) @RequestParam(required=false) List<Long> idValidationType,
			@ApiParam(value = "Min Expiry Date, all orders returned have expired on or after that date. Format 'yyyy-MM-dd' (UTC), null for no restriction", example = "2000-10-31", required = false) @RequestParam(required=false) String minExpiryDate,
			@ApiParam(value = "Max Expiry Date, all orders returned have expired on or before that date. Format 'yyyy-MM-dd' (UTC), null for no restriction", example = "2030-10-31", required = false) @RequestParam(required=false) String maxExpiryDate,
			@ApiParam(value = "Min Execution Likelihood, all orders returned have a at least the provided base quantity , Null = no restrictions", example = "0.00", required = false) @RequestParam(required=false) Double minExecutionLikelihood,
			@ApiParam(value = "Max Execution Likelihood, all orders returned have a at max the provided base quantity , Null = no restrictions", example = "1.00", required = false) @RequestParam(required=false) Double maxExecutionLikelihood,
    		@ApiParam(value = "Min limit price, Null = no restrictions", example = "500.00", required = false) @RequestParam(required=false) Double minLimitPrice,
    		@ApiParam(value = "Max limit price, Null = no restrictions", example = "1250.00", required = false) @RequestParam(required=false) Double maxLimitPrice,
			@ApiParam(value = "The ID of the order to be the first element according to the provided sort", example = "100003", required = false) @RequestParam(required=false) Long idFirstOrderIncluded,    		
    		@ApiIgnore("Ignored because swagger ui shows the wrong params, instead they are explained in the implicit params") Pageable pageable);

    @ApiOperation("Creation of a new Limit Order")
	@PostMapping(path = "/limitOrder", consumes=MediaType.APPLICATION_JSON_UTF8_VALUE)
	public long postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID has to be -1. The actual assigned Order ID is going to be returned. Version ID has to be 0", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder);
    
    @ApiOperation("Update of an existing Limit Order")
	@PutMapping("/limitOrder")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update. The version ID has to match the latest one for that order.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder);

    // reference order
    @Cacheable({"ReferenceOrder"})
    @ApiOperation("Retrieval of Reference Order Data")
	@GetMapping("/referenceOrder")
    @ApiImplicitParams ({
        @ApiImplicitParam(name = "page", dataType = "integer", paramType = "query",
                value = "Results page you want to retrieve (0..N)"),
        @ApiImplicitParam(name = "size", dataType = "integer", paramType = "query",
                value = "Number of records per page."),
        @ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query",
                value = "Sorting criteria in the format: property(,asc|desc). " +
                        "Default sort order is ascending. " +
                        "Multiple sort criteria are supported.")
    })
	public Page<OrderTo> getReferenceOrders (
			@ApiParam(value = "List of Order Type IDs, e.g. 13, 14", example = "13, 14", required = false) @RequestParam(required=false) List<Long> idOrderType,			
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
			@ApiParam(value = "List of IDs for users who have created the order, null = all orders, example 20026", example = "1, 2, 3", required = false) @RequestParam(required=false) List<Long> idCreatedByUser,
			@ApiParam(value = "Min Creation Date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minCreatedAtDate,
			@ApiParam(value = "Max Creation Date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxCreatedAtDate,
			@ApiParam(value = "List of IDs for users who have last updated the order, null = all orders, example 20026", example = "20026", required = false) @RequestParam(required=false) List<Long> idUpdatedByUser,
			@ApiParam(value = "Min last update date, all orders returned have been updated on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minLastUpdateDate,
			@ApiParam(value = "Max last update date, all orders returned have been updated on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLastUpdateDate,
			@ApiParam(value = "Min fill percentage, a real number between 0 and 1,  null for no restriction", example = "0.0", required = false) @RequestParam(required=false) Double minFillPercentage,
			@ApiParam(value = "Max fill percentage, a real number between 0 and 1,  null for no restriction", example = "1.0", required = false) @RequestParam(required=false) Double maxFillPercentage,			
			@ApiParam(value = "List of contract types, null = all orders, example 224, 225", example = "224, 225", required = false) @RequestParam(required=false) List<Long> idContractType,
			@ApiParam(value = "List of ticker IDs, null = all orders, example 231, 271", example = "231, 271", required = false) @RequestParam(required=false) List<Long> idTicker,
			// all above: order fields, all below: reference order fields
			@ApiParam(value = "Min metal price spread, Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double minMetalPriceSpread,
			@ApiParam(value = "Max metal price spread, Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double maxMetalPriceSpread,
			@ApiParam(value = "Min FX price spread, Null = no restrictions", example = "1.1", required = false) @RequestParam(required=false) Double minFxRateSpread,
			@ApiParam(value = "Max FX price spread, Null = no restrictions", example = "1.2", required = false) @RequestParam(required=false) Double maxFxRateSpread,
			@ApiParam(value = "Min Contango Backwardation, Null = no restrictions", example = "1.1", required = false) @RequestParam(required=false) Double minContangoBackwardation,
			@ApiParam(value = "Max Contango Backwardation, Null = no restrictions", example = "1.2", required = false) @RequestParam(required=false) Double maxContangoBackwardation,
			@ApiParam(value = "List of reference order leg ids, null = no restrictions", example = "", required = false) @RequestParam(required=false) List<Long> idLeg,
			@ApiParam(value = "Min leg notional, Null = no restrictions", example = "1.00", required = false) @RequestParam(required=false) Double minLegNotonal,
			@ApiParam(value = "Max leg notional, Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double maxLegNotional,
			@ApiParam(value = "Min leg fixing start date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minLegFixingStartDate,
			@ApiParam(value = "Max leg fixing start date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLegFixingStartDate,
			@ApiParam(value = "Min leg fixing end date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "1995-10-31 01:30:00", required = false) @RequestParam(required=false) String minLegFixingEndDate,
			@ApiParam(value = "Max leg fixing end date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLegFixingEndDate,
			@ApiParam(value = "List of leg payment offsets (symbolic date), null = no restriction, example 186, 187 ", example = "186, 187", required = false) @RequestParam(required=false) List<Long> idLegPaymentOffset,
			@ApiParam(value = "List of leg settlement currencies, null = no restriction, example 34,35,36,42,43", example = "34,35,36,42,43", required = false) @RequestParam(required=false) List<Long> idLegSettleCurrency,
			@ApiParam(value = "List of leg ref sources, null = no restriction, example 190, 191, 192, 193", example = "190, 191, 192, 193", required = false) @RequestParam(required=false) List<Long> idLegRefSource,
			@ApiParam(value = "List of leg index ref sources, null = no restriction, example 190, 191, 192, 193", example = "190, 191, 192, 193", required = false) @RequestParam(required=false) List<Long> idLegFxIndexRefSource,
			@ApiParam(value = "The ID of the order to be the first element according to the provided sort", example = "100003", required = false) @RequestParam(required=false) Long idFirstOrderIncluded,
			@ApiIgnore("Ignored because swagger ui shows the wrong params, instead they are explained in the implicit params") Pageable pageable);
        
    @ApiOperation("Creation of a new Reference Order")
	@PostMapping(path = "/referenceOrder" , consumes=MediaType.APPLICATION_JSON_UTF8_VALUE)
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
			@ApiParam(value = "The ID of the reference order leg", example = "1000002", required = true) @PathVariable(required=true) long legId);

    
    @ApiOperation("Update of an existing Reference Order Leg")
    @PutMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void updateReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "100003", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version.", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000002", required = true) @PathVariable(required=true) long legId,
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

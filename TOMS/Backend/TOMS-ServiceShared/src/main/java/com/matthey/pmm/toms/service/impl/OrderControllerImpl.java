package com.matthey.pmm.toms.service.impl;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsOrderService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderLegConverter;
import com.matthey.pmm.toms.service.exception.IllegalSortColumnException;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@PropertySource("classpath:mapping/Order.properties")
public abstract class OrderControllerImpl implements TomsOrderService {
	@Autowired
	protected Validator validator;
	
	@Autowired 
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;

	@Autowired
	protected ReferenceOrderLegRepository referenceOrderLegRepo;
	
	@Autowired
	protected LimitOrderConverter limitOrderConverter;

	@Autowired
	protected OrderRepository orderRepo;
	
	@Autowired
	protected ReferenceOrderConverter referenceOrderConverter;	

	@Autowired
	protected ReferenceOrderLegConverter referenceOrderLegConverter;	
		
	@Value("#{${search.mapping.abstractOrder}}")  
	private Map<String,String> abstractOrderSearchMap;
	
	@Value("#{${search.mapping.limitOrder}}")  
	private Map<String,String> limitOrderSearchMap;

	@Value("#{${search.mapping.referenceOrder}}")  
	private Map<String,String> referenceOrderSearchMap;
	
	
	// all order types
    @Override
    @ApiOperation("Retrieval of All Order Types")
	public List<OrderTo> getOrders (
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
    		@ApiIgnore("Ignored because swagger ui shows the wrong params, instead they are explained in the implicit params") Pageable pageable) {
		Date minCreatedAt = validator.verifyDateTime (minCreatedAtDate, getClass(), "getLimitOrders", "minCreatedAtDate");
		Date maxCreatedAt = validator.verifyDateTime (maxCreatedAtDate, getClass(), "getLimitOrders", "maxCreatedAtDate");
		Date minLastUpdate = validator.verifyDateTime (minLastUpdateDate, getClass(), "getLimitOrders", "minLastUpdateDate");
		Date maxLastUpdate = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getLimitOrders", "maxLastUpdateDate");

		Map<String,String> allMappings = new HashMap<String, String> ();
		allMappings.putAll(abstractOrderSearchMap);
		
		Pageable mappedPageable = Validator.verifySorts(pageable, getClass(), "getLimitOrders", "sort", allMappings);
		
		List<Order> matchingOrders = orderRepo.findByOrderIdAndOptionalParameters(noEmptyList(orderIds), noEmptyList(versionIds), noEmptyList(idInternalBu), 
				noEmptyList(idExternalBu), noEmptyList(idInternalLe), noEmptyList(idExternalLe),
				noEmptyList(idInternalPfolio), noEmptyList(idExternalPfolio), noEmptyList(idBuySell), 
				noEmptyList(idBaseCurrency), minBaseQuantity, maxBaseQuantity, noEmptyList(idBaseQuantityUnit), noEmptyList(idTermCurrency), reference,
				noEmptyList(idMetalForm), noEmptyList(idMetalLocation), noEmptyList(idOrderStatus), minCreatedAt, maxCreatedAt, minLastUpdate, maxLastUpdate,
				mappedPageable);
		return matchingOrders.stream()
				.map(x -> x instanceof LimitOrder?limitOrderConverter.toTo((LimitOrder)x):referenceOrderConverter.toTo((ReferenceOrder)x))
				.collect(Collectors.toList());
    }
		
	@Override
    @ApiOperation("Retrieval of Limit Order Data")
	public List<OrderTo> getLimitOrders (
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
    		@ApiParam(value = "Max limit price, Null = no restrictions", example = "1250.00", required = false) @RequestParam(required=false) Double maxLimitPrice,
    		@ApiIgnore("Ignored because swagger ui shows the wrong params, instead they are explained in the implicit params") Pageable pageable) {
	    // Validation of the input parameters required or not?
//		Optional<Party> intBu = validator.verifyParty(internalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT), getClass(), "getLimitOrders", "internalBuId", true);
//		Optional<Party> extBu = validator.verifyParty(externalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT), getClass(), "getLimitOrders", "externalBuId", true);
//		Optional<Reference> buySell = validator.verifyDefaultReference(buySellId, Arrays.asList(DefaultReferenceType.BUY_SELL), getClass(), "getLimitOrders", "buySellId", true);
		Date minCreatedAt = validator.verifyDateTime (minCreatedAtDate, getClass(), "getLimitOrders", "minCreatedAtDate");
		Date maxCreatedAt = validator.verifyDateTime (maxCreatedAtDate, getClass(), "getLimitOrders", "maxCreatedAtDate");
		Date minLastUpdate = validator.verifyDateTime (minLastUpdateDate, getClass(), "getLimitOrders", "minLastUpdateDate");
		Date maxLastUpdate = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getLimitOrders", "maxLastUpdateDate");
		Date minSettle = validator.verifyDateTime (minSettleDate, getClass(), "getLimitOrders", "minSettleDate");
		Date maxSettle = validator.verifyDateTime (maxSettleDate, getClass(), "getLimitOrders", "maxSettleDate");
		Date minStartConcrete = validator.verifyDateTime (minStartDateConcrete, getClass(), "getLimitOrders", "minStartDateConcrete");
		Date maxStartConcrete = validator.verifyDateTime (maxStartDateConcrete, getClass(), "getLimitOrders", "maxStartDateConcrete");
		Date minExpiry = validator.verifyDateTime (minExpiryDate, getClass(), "getLimitOrders", "minExpiryDate");
		Date maxExpiry = validator.verifyDateTime (maxExpiryDate, getClass(), "getLimitOrders", "maxExpiryDate");
		
		Map<String,String> allMappings = new HashMap<String, String> ();
		allMappings.putAll(abstractOrderSearchMap);
		allMappings.putAll(limitOrderSearchMap);
				
		Pageable mappedPageable = Validator.verifySorts(pageable, getClass(), "getLimitOrders", "sort", allMappings);
		
		List<LimitOrder> matchingOrders = limitOrderRepo.findByOrderIdAndOptionalParameters(noEmptyList(orderIds), noEmptyList(versionIds), noEmptyList(idInternalBu), 
				noEmptyList(idExternalBu), noEmptyList(idInternalLe), noEmptyList(idExternalLe),
				noEmptyList(idInternalPfolio), noEmptyList(idExternalPfolio), noEmptyList(idBuySell), 
				noEmptyList(idBaseCurrency), minBaseQuantity, maxBaseQuantity, noEmptyList(idBaseQuantityUnit), noEmptyList(idTermCurrency), reference,
				noEmptyList(idMetalForm), noEmptyList(idMetalLocation), noEmptyList(idOrderStatus), minCreatedAt, maxCreatedAt, minLastUpdate, maxLastUpdate,
				// all above: order fields, all below: limit order fields
				minSettle, maxSettle, minStartConcrete, maxStartConcrete, noEmptyList(idStartDateSymbolic), noEmptyList(idPriceType), noEmptyList(idYesNoPartFillable), 
				noEmptyList(idStopTriggerType), noEmptyList(idCurrencyCrossMetal),
				noEmptyList(idValidationType), minExpiry, maxExpiry, minExecutionLikelihood, maxExecutionLikelihood, minLimitPrice, maxLimitPrice,
				mappedPageable);
		return matchingOrders.stream()
				.map(x -> limitOrderConverter.toTo(x))
				.collect(Collectors.toList());
	}

	@ApiOperation("Creation of a new Limit Order")
	public long postLimitOrder (@ApiParam(value = "The new Limit Order. Order ID and version have to be 0. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) LimitOrderTo newLimitOrder) {
    	// validation checks
    	validator.validateLimitOrderFields (this.getClass(), "postLimitOrder", "newLimitOrder", newLimitOrder, true, null);
	
		LimitOrder managedEntity = limitOrderConverter.toManagedEntity(newLimitOrder);
		managedEntity.setCreatedAt(new Date());
		managedEntity = limitOrderRepo.save(managedEntity);
    	return managedEntity.getOrderId();
    }
    
    @ApiOperation("Update of an existing Limit Order")
	public void updateLimitOrder (@ApiParam(value = "The Limit Order to update. Order ID has to denote an existing Limit Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) LimitOrderTo existingLimitOrder) {
    	// identify the existing limit order
    	Optional<LimitOrder> oldLimitOrderManaged = limitOrderRepo.findLatestByOrderId(existingLimitOrder.id());
    	
    	if (oldLimitOrderManaged.isEmpty()) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrder", "existingLimitOrder.id" , "Limit Order", "" + existingLimitOrder.id());
    	}
    	validator.validateLimitOrderFields (this.getClass(), "updateLimitOrder", "existingLimitOrder", existingLimitOrder, false, 
    			limitOrderConverter.toTo(oldLimitOrderManaged.get()));

    	LimitOrder existingLimitOrderManaged = limitOrderConverter.toManagedEntity(existingLimitOrder);
    	existingLimitOrderManaged.setVersion(existingLimitOrderManaged.getVersion()+1);
    	limitOrderRepo.save(existingLimitOrderManaged);
    }
	
	@Override
    @ApiOperation("Retrieval of Reference Order Data")
    @Cacheable({"ReferenceOrder"})
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
	public List<OrderTo> getReferenceOrders (
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
			// all above: order fields, all below: reference order fields
			@ApiParam(value = "Min metal price spread, Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double minMetalPriceSpread,
			@ApiParam(value = "Max metal price spread, Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double maxMetalPriceSpread,
			@ApiParam(value = "Min FX price spread, Null = no restrictions", example = "1.1", required = false) @RequestParam(required=false) Double minFxRateSpread,
			@ApiParam(value = "Max FX price spread, Null = no restrictions", example = "1.2", required = false) @RequestParam(required=false) Double maxFxRateSpread,
			@ApiParam(value = "Min Contango Backwardation, Null = no restrictions", example = "1.1", required = false) @RequestParam(required=false) Double minContangoBackwardation,
			@ApiParam(value = "Max Contango Backwardation, Null = no restrictions", example = "1.2", required = false) @RequestParam(required=false) Double maxContangoBackwardation,
			@ApiParam(value = "List of contract types, null = all orders, example 224, 225", example = "224, 225", required = false) @RequestParam(required=false) List<Long> idContractType,
			@ApiParam(value = "List of reference order leg ids, null = no restrictions", example = "", required = false) @RequestParam(required=false) List<Long> idLeg,
			@ApiParam(value = "Min leg notional, Null = no restrictions", example = "1.00", required = false) @RequestParam(required=false) Double minLegNotonal,
			@ApiParam(value = "Max leg notional, Null = no restrictions", example = "1000.00", required = false) @RequestParam(required=false) Double maxLegNotional,
			@ApiParam(value = "Min leg fixing start date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minLegFixingStartDate,
			@ApiParam(value = "Max leg fixing start date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLegFixingStartDate,
			@ApiParam(value = "Min leg fixing end date, all orders returned have been created on or after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2000-10-31 01:30:00", required = false) @RequestParam(required=false) String minLegFixingEndDate,
			@ApiParam(value = "Max leg fixing end date, all orders returned have been created on or before that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC), null for no restriction", example = "2030-10-31 01:30:00", required = false) @RequestParam(required=false) String maxLegFixingEndDate,
			@ApiParam(value = "List of leg payment offsets (symbolic date), null = no restriction, example 186, 187 ", example = "186, 187", required = false) @RequestParam(required=false) List<Long> idLegPaymentOffset,
			@ApiParam(value = "List of leg settlement currencies, null = no restriction, example 34,35,36,42,43", example = "34,35,36,42,43", required = false) @RequestParam(required=false) List<Long> idLegSettleCurrency,
			@ApiParam(value = "List of leg ref sources, null = no restriction, example 190, 191, 192, 193", example = "190, 191, 192, 193", required = false) @RequestParam(required=false) List<Long> idLegRefSource,
			@ApiParam(value = "List of leg index ref sources, null = no restriction, example 190, 191, 192, 193", example = "190, 191, 192, 193", required = false) @RequestParam(required=false) List<Long> idLegFxIndexRefSource ,
			@ApiIgnore("Ignored because swagger ui shows the wrong params, instead they are explained in the implicit params") Pageable pageable) {
		// validation of input required or not?
//		Optional<Party> intBu = validator.verifyParty(internalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT), getClass(), "getReferenceOrders", "internalBuId", true);
//		Optional<Party> extBu = validator.verifyParty(externalBuId, Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT), getClass(), "getReferenceOrders", "externalBuId", true);
//		Optional<Reference> buySell = validator.verifyDefaultReference(buySellId, Arrays.asList(DefaultReferenceType.BUY_SELL), getClass(), "getReferenceOrders", "buySellId", true); 

		Date minCreatedAt = validator.verifyDateTime (minCreatedAtDate, getClass(), "getReferenceOrders", "minCreatedAtDate");
		Date maxCreatedAt = validator.verifyDateTime (maxCreatedAtDate, getClass(), "getReferenceOrders", "maxCreatedAtDate");
		Date minLastUpdate = validator.verifyDateTime (minLastUpdateDate, getClass(), "getReferenceOrders", "minLastUpdateDate");
		Date maxLastUpdate = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getReferenceOrders", "maxLastUpdateDate");
		Date minLegFixingStart = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getReferenceOrders", "minLegFixingStartDate");
		Date maxLegFixingStart = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getReferenceOrders", "maxLegFixingStartDate");
		Date minLegFixingEnd = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getReferenceOrders", "minLegFixingEndDate");
		Date maxLegFixingEnd = validator.verifyDateTime (maxLastUpdateDate, getClass(), "getReferenceOrders", "maxLegFixingEndDate");
		
		Map<String,String> allMappings = new HashMap<String, String> ();
		allMappings.putAll(abstractOrderSearchMap);
		allMappings.putAll(referenceOrderSearchMap);	
		
		Pageable mappedPageable = Validator.verifySorts(pageable,  getClass(), "getReferenceOrders", "sort", allMappings);
		
		Page<ReferenceOrder> matchingOrders = referenceOrderRepo.findByOrderIdAndOptionalParameters(noEmptyList(orderIds), noEmptyList(versionIds), noEmptyList(idInternalBu), 
				noEmptyList(idExternalBu), noEmptyList(idInternalLe), noEmptyList(idExternalLe),
				noEmptyList(idInternalPfolio), noEmptyList(idExternalPfolio), noEmptyList(idBuySell), 
				noEmptyList(idBaseCurrency), minBaseQuantity, maxBaseQuantity, noEmptyList(idBaseQuantityUnit), noEmptyList(idTermCurrency), reference,
				noEmptyList(idMetalForm), noEmptyList(idMetalLocation), noEmptyList(idOrderStatus), minCreatedAt, maxCreatedAt, minLastUpdate, maxLastUpdate,
				// all above: order fields, all below: reference order fields
				minMetalPriceSpread, maxMetalPriceSpread, minFxRateSpread, maxFxRateSpread, minContangoBackwardation, maxContangoBackwardation,
				noEmptyList(idContractType), noEmptyList(idLeg), minLegNotonal, maxLegNotional, minLegFixingStart, 
				maxLegFixingStart, minLegFixingEnd, maxLegFixingEnd, 
				noEmptyList(idLegPaymentOffset), noEmptyList(idLegSettleCurrency), noEmptyList(idLegRefSource), noEmptyList(idLegFxIndexRefSource),
				mappedPageable
				);
				
		return matchingOrders.stream()
				.map(x -> referenceOrderConverter.toTo(x))
				.collect(Collectors.toList());
	}
    
    @ApiOperation("Creation of a new Reference Order")
	public long postReferenceOrder (@ApiParam(value = "The new Reference Order. Order ID and version have to be 0. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo newReferenceOrder) {
    	// validation checks
    	validator.validateReferenceOrderFields (this.getClass(), "postReferenceOrder", "newReferenceOrder", newReferenceOrder, true, null);
	
		ReferenceOrder managedEntity = referenceOrderConverter.toManagedEntity(newReferenceOrder);
		managedEntity.setCreatedAt(new Date());
		managedEntity = referenceOrderRepo.save(managedEntity);
    	return managedEntity.getOrderId();
    }
    
    @ApiOperation("Update of an existing Reference Order")
	public void updateReferenceOrder (@ApiParam(value = "The Reference Order to update. Order ID has to denote an existing Reference Order in a valid state for update.", example = "", required = true) @RequestBody(required=true) ReferenceOrderTo existingReferenceOrder) {
    	Optional<ReferenceOrder> oldOrderManaged = referenceOrderRepo.findLatestByOrderId(existingReferenceOrder.id());
    	
    	if (oldOrderManaged.isEmpty()) {
    		throw new UnknownEntityException (this.getClass(), "updateReferenceOrder", "existingReferenceOrder.id" , "Referencet Order", "" + existingReferenceOrder.id());
    	}
    	validator.validateReferenceOrderFields (this.getClass(), "updateReferenceOrder", "existingReferenceOrder", existingReferenceOrder, false, 
    			referenceOrderConverter.toTo(oldOrderManaged.get()));

    	ReferenceOrder existingOrderManaged = referenceOrderConverter.toManagedEntity(existingReferenceOrder);
    	existingOrderManaged.setVersion(existingOrderManaged.getVersion()+1);
    	referenceOrderRepo.save(existingOrderManaged);
    }
    
    @Cacheable({"ReferenceOrderLeg"})
    @ApiOperation("Retrieval of all reference order legs for a single reference order.")
	@GetMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs")
	public Set<ReferenceOrderLegTo> getReferenceOrderLegs (@ApiParam(value = "The ID of the reference order", example = "1000001", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order", example = "1", required = true) @PathVariable(required=true) int version) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "getReferenceOrderLegs", "referenceOrderId", false);
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "getReferenceOrderLegs", "version");
    	return orderManaged.get().getLegs().stream()
    		.map(x -> referenceOrderLegConverter.toTo(x))
    		.collect(Collectors.toSet());
    }
    
    @ApiOperation("Creation of a new Reference Order Leg")
	@PostMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs")
	public long postReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "1000001", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The new Reference Order Leg, ID has to be -1. The actual assigned ID is going to be returned.", example = "", required = true) @RequestBody(required=true) ReferenceOrderLegTo newReferenceOrderLeg) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "postReferenceOrderLeg", "referenceOrderId", false);
    	if (orderManaged.get().getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
    		throw new IllegalStateException(getClass(), "postReferenceOrderLeg", "referenceOrderId", 
    				orderManaged.get().getOrderStatus().toString(), "ReferenceOrder", DefaultOrderStatus.REFERENCE_ORDER_PENDING.name());
    	}
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "postReferenceOrderLeg", "version");
    	validator.validateReferenceOrderLegFields (this.getClass(), "postReferenceOrderLeg", "newReferenceOrderLeg", orderManaged.get(), newReferenceOrderLeg,  false, null);
    	ReferenceOrderLeg newLegManaged = referenceOrderLegConverter.toManagedEntity(newReferenceOrderLeg);
    	orderManaged.get().getLegs().add(newLegManaged);
    	referenceOrderRepo.save(orderManaged.get());
    	return newLegManaged.getId();
    }

    @ApiOperation("Deletion of an existing Reference Order Leg")
	@DeleteMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void deleteReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "1000001", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000001", required = true) @PathVariable(required=true) int legId) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "deleteReferenceOrderLeg", "referenceOrderId", false);
    	if (orderManaged.get().getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
    		throw new IllegalStateException(getClass(), "deleteReferenceOrderLeg", "referenceOrderId", 
    				orderManaged.get().getOrderStatus().toString(), "ReferenceOrder", DefaultOrderStatus.REFERENCE_ORDER_PENDING.name());
    	}
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "deleteReferenceOrderLeg", "version");
    	orderManaged.get().getLegs().removeIf(x -> x.getId() == legId);
    	referenceOrderRepo.save(orderManaged.get());
    	// remove leg from association with order/orderversion, but keep leg entity.
    	// reason: leg might be used in an older version of the order as well.
    }
    
    @ApiOperation("Update of an existing Reference Order Leg")
    @PutMapping("/referenceOrder/{referenceOrderId}/version/{version}/legs/{legId}")
	public void updateReferenceOrderLeg (@ApiParam(value = "The ID of the reference order", example = "100005", required = true) @PathVariable(required=true) Long referenceOrderId,
			@ApiParam(value = "The version of the reference order, has to be the latest version.", example = "1", required = true) @PathVariable(required=true) int version,
			@ApiParam(value = "The ID of the reference order leg", example = "1000001", required = true) @PathVariable(required=true) int legId,
			@ApiParam(value = "The new Reference Order Leg, ID has to be matching legId.", example = "", required = true) @RequestBody(required=true) ReferenceOrderLegTo existingReferenceOrderLeg) {
    	Optional<ReferenceOrder> orderManaged = validator.verifyReferenceOrderId(referenceOrderId, this.getClass(), "updateReferenceOrderLeg", "referenceOrderId", false);
    	if (orderManaged.get().getOrderStatus().getId() != DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id()) {
    		throw new IllegalStateException(getClass(), "updateReferenceOrderLeg", "referenceOrderId", 
    				orderManaged.get().getOrderStatus().toString(), "ReferenceOrder", DefaultOrderStatus.REFERENCE_ORDER_PENDING.name());
    	}
    	validator.verifyReferenceOrderVersion (orderManaged.get(), version, this.getClass(), "updateReferenceOrderLeg", "version");
    	Optional<ReferenceOrderLeg> existingLeg = validator.verifyReferenceOrderLegId(legId, this.getClass(), "postReferenceOrderLeg", "legId", false);
    	validator.validateReferenceOrderLegFields (this.getClass(), "postReferenceOrderLeg", "newReferenceOrderLeg", orderManaged.get(), existingReferenceOrderLeg,  true, existingLeg.get());
    	ReferenceOrderLeg updatedLeg = referenceOrderLegConverter.toManagedEntity(existingReferenceOrderLeg);
    	referenceOrderLegRepo.save(updatedLeg);
    }
    
    @ApiOperation("Retrieve the value of an attribute of the provided Limit Order based on the other fields of the order")
    public String getAttributeValueLimitOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "settleDate", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) LimitOrderTo orderTemplate) {
    	return TomsService.applyAttributeCalculation(orderTemplate, attributeName);
    }

	@ApiOperation("Retrieve the value of an attribute of the provided Reference Order based on the other fields of the order")
    public String getAttributeValueReferenceOrder (@ApiParam(value = "The name of the attribute value is supposed to be retrieved for", example = "settleDate", required = true) @RequestParam(required=false) String attributeName,
    		@ApiParam(value = "The current order those value should be retrieved", example = "") @RequestBody(required=true) ReferenceOrderTo orderTemplate) {
    	return TomsService.applyAttributeCalculation(orderTemplate, attributeName);
    }
	
    private <T> List<T> noEmptyList(List<T> list) {
    	if (list != null && list.size() == 0) {
    		return null;
    	}
    	return list;
	}
}
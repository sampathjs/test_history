package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.service.TomsFillService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.mock.testdata.TestFill;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ImmutableFillTo;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockFillController implements TomsFillService {
	public static final AtomicInteger ID_COUNTER_FILL = new AtomicInteger(50000);
	public static final List<FillTo> CUSTOM_ORDER_FILLS = new CopyOnWriteArrayList<>();
	
    @ApiOperation("Retrieval of a single fill for a Limit Order")
    public FillTo getLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId, 
    		@ApiParam(value = "The fill ID belonging to the order having limitOrderId", example = "1") @PathVariable int fillId) {
    	Stream<OrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());

    	LimitOrderTo limitOrder = SharedMockLogic.validateOrderId(this.getClass(), "getLimitOrderFill", "limitOrderId", limitOrderId, allDataSources, LimitOrderTo.class);
    	if (limitOrder.fillIds() == null || !limitOrder.fillIds().contains(fillId) ) {
    		throw new IllegalIdException(this.getClass(), "getLimitOrderFill", "fillId", limitOrder.fillIds().toString(), "" + fillId);
    	}
    	Stream<FillTo> allDataSourcesFills = Stream.concat(TestFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	List<FillTo> fills = allDataSourcesFills
    		.filter(x -> x.id() == fillId)
    		.collect(Collectors.toList());
    	if (fills.size() != 1) {
    		throw new IllegalIdException(this.getClass(), "getLimitOrderFill", "fillId", limitOrder.fillIds().toString(), "" + fillId);    		
    	}
    	return fills.get(0);
    }
    
    @ApiOperation("Retrieval of all fills for a Limit Order")
    public Set<FillTo> getLimitOrderFills (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId) {
    	Stream<OrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateOrderId(this.getClass(), "getLimitOrderFills", "limitOrderId", limitOrderId, allDataSources, LimitOrderTo.class);
    	Stream<FillTo> allOrderFills = Stream.concat(TestFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	
    	if (limitOrder.fillIds() != null) {
    		return allOrderFills
    				.filter(x -> limitOrder.fillIds().contains(x.id()))
    				.collect(Collectors.toSet());
    	} else {
    		return new HashSet<>();
    	}
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    public int postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	Stream<OrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateOrderId (this.getClass(), "getLimitOrderFill", "limitOrderId", limitOrderId, allDataSources, LimitOrderTo.class);
    	// validation checks
    	SharedMockLogic.validateFillFields (this.getClass(), "postLimitOrderFill", "newOrderFill", newOrderFill, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

		FillTo withId = ImmutableFillTo.copyOf(newOrderFill)
    			.withId(ID_COUNTER_FILL.incrementAndGet())
    			.withLastUpdateDateTime(sdfDateTime.format(new Date()));
		CUSTOM_ORDER_FILLS.add (withId);		
		
		List<TestLimitOrder> enumList = TestLimitOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == limitOrderId)
			.collect(Collectors.toList());

		List<Integer> newfillIds = new ArrayList<>(limitOrder.fillIds().size()+1);
		newfillIds.addAll (limitOrder.fillIds());
		newfillIds.add(withId.id());

		LimitOrderTo updatedLimitOrder = ImmutableLimitOrderTo.copyOf(limitOrder)
				.withFillIds(newfillIds)
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestLimitOrder order = enumList.get(0);
	    	order.setEntity (updatedLimitOrder);
		} else {
			MockOrderController.CUSTOM_ORDERS.remove(limitOrder);			
			MockOrderController.CUSTOM_ORDERS.add(updatedLimitOrder);
		}
		return withId.id();
    }
    	
    @ApiOperation("Retrieval of a the fill for a Reference Order, if present")
    public FillTo getReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId) {
    	Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
    	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId(this.getClass(), "getReferenceOrderFill", "referenceOrderId", referenceOrderId, allDataSources);
    	
    	if (referenceOrder.fillId() == null) {
    		return null;
    	}
    	
    	Stream<FillTo> allDataSourcesFill = Stream.concat(TestFill.asList().stream(), CUSTOM_ORDER_FILLS.stream());
    	List<FillTo> orderFills = allDataSourcesFill
    			.filter( x -> x.id() == referenceOrder.fillId())
    			.collect(Collectors.toList());
    	return orderFills.get(0);    	
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    public int postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "1") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
       	Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
       	ReferenceOrderTo referenceOrder = SharedMockLogic.validateReferenceOrderId (this.getClass(), "postLimitOrderFill", "referenceOrderId", referenceOrderId, allDataSources);
       	if (referenceOrder.fillId() != null && referenceOrder.fillId() > 0) {
       		throw new IllegalStateException (this.getClass(), "postReferenceOrderFill", "newOrderFill", 
       				"fill for Reference Order having fill already installed",
       				"Reference Order #" + referenceOrderId + " already assigned to fill ID #" + referenceOrder.fillId(),
       				"fill not assigned");
       	}
    	// validation checks
    	SharedMockLogic.validateFillFields (this.getClass(), "postReferenceOrderFill", "newOrderFill", newOrderFill, true, null);
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);

		FillTo withId = ImmutableFillTo.copyOf(newOrderFill)
    			.withId(ID_COUNTER_FILL.incrementAndGet())
    			.withLastUpdateDateTime(sdfDateTime.format(new Date()));
		CUSTOM_ORDER_FILLS.add (withId);		
		
		List<TestReferenceOrder> enumList = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == referenceOrderId)
			.collect(Collectors.toList());

		ReferenceOrderTo updatedReferenceOrder = ImmutableReferenceOrderTo.copyOf(referenceOrder)
				.withFillId(withId.id())
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestReferenceOrder order = enumList.get(0);
	    	order.setEntity (updatedReferenceOrder);
		} else {
			MockOrderController.CUSTOM_ORDERS.remove(referenceOrder);			
			MockOrderController.CUSTOM_ORDERS.add(updatedReferenceOrder);
		}
		return withId.id();    	
    }
}
package com.matthey.pmm.toms.service.mock;


import java.util.Date;
import java.util.Optional;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.service.impl.FillControllerImpl;
import com.matthey.pmm.toms.transport.FillTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockFillController extends FillControllerImpl {
	
    @ApiOperation("Creation of a new fills for a Limit Order")
    @Override
    public long postLimitOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	long newFillId = super.postLimitOrderFill(limitOrderId, newOrderFill);
    	Optional<LimitOrder> limitOrder = validator.verifyLimitOrderId(limitOrderId, getClass(), "postLimitOrderFill", "limitOrderId", false);
    	
    	
		limitOrderRepo.save(limitOrder.get());
		return newFillId;
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    @Override
    public long postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be -1. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	long newFillId = super.postReferenceOrderFill(referenceOrderId, newOrderFill);
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "postReferenceOrderFill", "referenceOrderId", false);

    	referenceOrderRepo.save(referenceOrder.get());
		return newFillId;
    }
}
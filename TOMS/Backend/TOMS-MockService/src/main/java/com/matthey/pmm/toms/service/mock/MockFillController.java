package com.matthey.pmm.toms.service.mock;


import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.service.impl.FillControllerImpl;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ImmutableFillTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Transactional
public class MockFillController extends FillControllerImpl {
	
	// this simulates either a fail or a success on Endur side deal booking.
    @ApiOperation("Creation of a new fills for a Limit Order")
    @Override
    public long postLimitOrderFill(
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new fill. ID has to be 0. The actual assigned Order ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	long newFillId = super.postLimitOrderFill(limitOrderId, newOrderFill);
		newOrderFill = ImmutableFillTo.builder()
				.from(newOrderFill)
				.id(newFillId)
				.idFillStatus(DefaultReference.FILL_STATUS_PROCESSING.getEntity().id())
				.build();    	
    	updateLimitOrderFill(limitOrderId, newFillId, newOrderFill);		
    	
    	if (Math.random() >= 0.9d) {
    		newOrderFill = ImmutableFillTo.builder()
    				.from(newOrderFill)
    				.idFillStatus(DefaultReference.FILL_STATUS_FAILED.getEntity().id())
    				.errorMessage("Endur Deal Booking Process Failed")
    				.build();
    	}  else {
    		Optional<Fill> fillWithMaxTradeId = fillRepo.findTopByOrderByTradeIdDesc();
    		newOrderFill = ImmutableFillTo.builder()
    				.from(newOrderFill)
    				.idTrade(fillWithMaxTradeId.isPresent()?fillWithMaxTradeId.get().getTradeId()+7:1000000l)
    				.idFillStatus(DefaultReference.FILL_STATUS_COMPLETED.getEntity().id())
    				.build(); 
    	}
    	updateLimitOrderFill(limitOrderId, newFillId, newOrderFill);		
		return newFillId;
    }
    
    @ApiOperation("Creation of a new fills for a Limit Order")
    @Override
    public long postReferenceOrderFill (
    		@ApiParam(value = "The order ID of the order the fill object is to be retrieved from", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new fill. ID has to be 0. The actual assigned fill ID is going to be returned", example = "", required = true) @RequestBody(required=true) FillTo newOrderFill) {
    	long newFillId = super.postReferenceOrderFill(referenceOrderId, newOrderFill);
    	Optional<ReferenceOrder> referenceOrder = validator.verifyReferenceOrderId(referenceOrderId, getClass(), "postReferenceOrderFill", "referenceOrderId", false);
		newOrderFill = ImmutableFillTo.builder()
				.from(newOrderFill)
				.id(newFillId)
				.idFillStatus(DefaultReference.FILL_STATUS_PROCESSING.getEntity().id())
				.build();
    	updateReferenceOrderFill(referenceOrderId, newFillId, newOrderFill);
    	if (Math.random() >= 0.9d) {
    		newOrderFill = ImmutableFillTo.builder()
    				.from(newOrderFill)
    				.idFillStatus(DefaultReference.FILL_STATUS_FAILED.getEntity().id())
    				.errorMessage("Endur Deal Booking Process Failed")
    				.build();
    	} else {
    		Optional<Fill> fillWithMaxTradeId = fillRepo.findTopByOrderByTradeIdDesc();
    		newOrderFill = ImmutableFillTo.builder()
    				.from(newOrderFill)
    				.idTrade(fillWithMaxTradeId.isPresent()?fillWithMaxTradeId.get().getTradeId()+7:1000000l)
    				.idFillStatus(DefaultReference.FILL_STATUS_COMPLETED.getEntity().id())
    				.build();
    	}
    	updateReferenceOrderFill(referenceOrderId, newFillId, newOrderFill);		
		return newFillId;
    }
}
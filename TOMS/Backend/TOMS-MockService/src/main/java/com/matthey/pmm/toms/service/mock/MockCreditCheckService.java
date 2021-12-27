package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.impl.CreditCheckServiceImpl;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockCreditCheckService extends CreditCheckServiceImpl {
	
	// this simulates either a fail or a success on Endur side deal booking.
    @ApiOperation("Creation of a new Credit Check for a Limit Order")
    @Override
    public long postLimitOrderCreditCheck(
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "100001") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new Credit Check. ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
    	long newCreditCheckId = super.postLimitOrderCreditCheck(limitOrderId, newCreditCheck);
    	LimitOrder order = limitOrderRepo.findLatestByOrderId(limitOrderId).get();
    	newCreditCheck = getCreditCheckOutcome(newCreditCheck, order, newCreditCheckId);
    	updateLimitOrderCreditCheck(limitOrderId, newCreditCheckId, newCreditCheck);		
		return newCreditCheckId;
    }
    
    @ApiOperation("Creation of a new Credit Check for a Reference Order")
    @Override
    public long postReferenceOrderCreditCheck (
    		@ApiParam(value = "The order ID of the reference order the Credit Check is to be posted for ", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new Credit Check . ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) CreditCheckTo newCreditCheck) {
    	long newCreditCheckId = super.postReferenceOrderCreditCheck(referenceOrderId, newCreditCheck);
    	ReferenceOrder order = referenceOrderRepo.findLatestByOrderId(referenceOrderId).get();
    	newCreditCheck = getCreditCheckOutcome(newCreditCheck, order, newCreditCheckId);
    	updateReferenceOrderCreditCheck(referenceOrderId, newCreditCheckId, newCreditCheck);		
		return newCreditCheckId;
    }

	private CreditCheckTo getCreditCheckOutcome(CreditCheckTo newCreditCheck, Order order, long newCreditCheckId) {
    	SimpleDateFormat sdf = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		if (Math.random() >= 0.9d) { // run fail for technical problems (10%) or not   		
        	if (Math.random() >= 0.5d) { // credit check passes or not
            	if (Math.random() >= 0.5d) { // credit check does not pass for AR nor NOT AR
            		newCreditCheck = ImmutableCreditCheckTo.builder()
            				.from(newCreditCheck)
            				.id(newCreditCheckId)
            				.idCreditCheckRunStatus(DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED.getEntity().id())
            				.idCreditCheckOutcome(DefaultReference.CREDIT_CHECK_OUTCOME_FAILED.getEntity().id())
            				.currentUtilization(order.getBaseQuantity()+1)
            				.runDateTime(sdf.format(new Date()))
            				.creditLimit(order.getBaseQuantity())
            				.build();  		
            	} else { // credit check does not pass for AR reasons
            		newCreditCheck = ImmutableCreditCheckTo.builder()
            				.from(newCreditCheck)
            				.id(newCreditCheckId)
            				.idCreditCheckRunStatus(DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED.getEntity().id())
            				.idCreditCheckOutcome(DefaultReference.CREDIT_CHECK_OUTCOME_AR_FAILED.getEntity().id())
            				.currentUtilization(order.getBaseQuantity()+1)
            				.runDateTime(sdf.format(new Date()))
            				.creditLimit(order.getBaseQuantity())
            				.build();
            	} 
        	} else { // credit check passes
        		newCreditCheck = ImmutableCreditCheckTo.builder()
        				.from(newCreditCheck)
        				.id(newCreditCheckId)
        				.idCreditCheckRunStatus(DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED.getEntity().id())
        				.idCreditCheckOutcome(DefaultReference.CREDIT_CHECK_OUTCOME_PASSED.getEntity().id())
        				.currentUtilization(0d)
        				.runDateTime(sdf.format(new Date()))
        				.creditLimit(order.getBaseQuantity()*1000)
        				.build();  
        	}
    	} else { // credit check fails for technical reasons
    		newCreditCheck = ImmutableCreditCheckTo.builder()
    				.from(newCreditCheck)
    				.id(newCreditCheckId)
    				.idCreditCheckRunStatus(DefaultReference.CREDIT_CHECK_RUN_STATUS_FAILED.getEntity().id())
    				.runDateTime(sdf.format(new Date()))
    				.build();   
    	}
		return newCreditCheck;
	}

}
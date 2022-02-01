package com.matthey.pmm.toms.service.mock;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.service.impl.OrderControllerImpl;
import com.matthey.pmm.toms.transport.OrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockOrderController extends OrderControllerImpl {	

	@Autowired
	protected OrderTestDataGenerator testDataGenerator;
	
    @ApiOperation("Creation of test deals for performance testing (Mock Interface Only)")
	@GetMapping("/testOrders")
	public Set<OrderTo> createTestOrders (
			@ApiParam(value = "The number of deals to be created.", example = "1000", required = true) @RequestParam(required=true) int numberOfDeals) {
    	Set<Order> newTestOrders = new HashSet<>();
    	for (int i=0; i < numberOfDeals; i++) {
    		Order order = null;
    		if (Math.random() >= 0.5d) {
    			order = testDataGenerator.createTestLimitOrder ();
        		newTestOrders.add(order);
        		while (Math.random() > 0.75d) {
        			order = testDataGenerator.createNewVersion (order);
        			newTestOrders.add(order);
        		}
    		} else {
    			order = testDataGenerator.createTestReferenceOrder ();
        		newTestOrders.add(order);
        		while (Math.random() > 0.75d) {
        			order = testDataGenerator.createNewVersion (order);
        			newTestOrders.add(order);
        		}
    		}
    	}
    	return newTestOrders.stream()
    			.map(x -> (x instanceof LimitOrder)?limitOrderConverter.toTo((LimitOrder)x):referenceOrderConverter.toTo((ReferenceOrder)x))
    			.collect(Collectors.toSet());
    }


}
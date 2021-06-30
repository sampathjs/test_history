package com.matthey.pmm.toms.service.mock;


import java.text.SimpleDateFormat;
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

import com.matthey.pmm.toms.service.TomsOrderCommentService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestOrderComment;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableOrderCommentTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockOrderCommentController implements TomsOrderCommentService {
	public static final AtomicInteger ID_COUNTER_ORDER_COMMENT = new AtomicInteger(10000);
	public static final List<OrderCommentTo> CUSTOM_ORDER_COMMENTS = new CopyOnWriteArrayList<>();
    
    @ApiOperation("Retrieval of the comment data for a Limit Order")
    public Set<OrderCommentTo> getCommentsLimitOrders (
    		@ApiParam(value = "The order ID of the limit order the comments are to be retrieved from", example = "1") @PathVariable int limitOrderId) {
    	return  getOrderComments (limitOrderId, "getCommentsLimitOrders", LimitOrderTo.class);
    }
    
    @ApiOperation("Creation of a new comment for a Limit Order")
    public int postLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be posted for", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new comment. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment) {
    	return postOrderComment (limitOrderId, newComment, "postLimitOrderComment", LimitOrderTo.class);
    }

    @ApiOperation("Update of a comment for a Limit Order")
    public void updateLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1") @PathVariable int commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment) {
    	updateOrderComment (limitOrderId, commentId, existingComment, "updateLimitOrderComment", LimitOrderTo.class);
    }
    
    @ApiOperation("Retrieval of a comment  for a Limit Order")
    public OrderCommentTo getCommentLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the comment is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1") @PathVariable int commentId) {
    	return getOrderComment (limitOrderId, commentId, "getCommentLimitOrder", LimitOrderTo.class);
    }
    
    @ApiOperation("Deletion of a comment for a Limit Order")
    public void deleteLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1") @PathVariable int commentId) {
    	deleteOrderComment (limitOrderId, commentId, "deleteLimitOrder", LimitOrderTo.class);
    }
    
    @ApiOperation("Retrieval of the comment data for a reference Order")
    public Set<OrderCommentTo> getCommentsReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the comments are to be retrieved from", example = "1001") @PathVariable int referenceOrderId) {
    	return getOrderComments (referenceOrderId, "getCommentsReferenceOrders", ReferenceOrderTo.class);
    }

    @ApiOperation("Retrieval of a comment  for a reference Order")
    public OrderCommentTo getCommentReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the comment is to be retrieved from", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1") @PathVariable int commentId) {
    	return getOrderComment (referenceOrderId, commentId, "getCommentReferenceOrder", ReferenceOrderTo.class);
    }

    @ApiOperation("Creation of a new comment for a reference Order")
    public int postReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be posted for", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new comment. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment) {
    	return postOrderComment (referenceOrderId, newComment, "postReferenceOrderComment", ReferenceOrderTo.class);
    }

    @ApiOperation("Update of a comment for a reference Order")
    public void updateReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be updated for ", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1001") @PathVariable int commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment) {
    	updateOrderComment (referenceOrderId, commentId, existingComment, "updateReferenceOrderComment", ReferenceOrderTo.class);
    }
    
    @ApiOperation("Deletion of a comment for a Reference Order")
    public void deleteReferenceOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1") @PathVariable int commentId) {
    	deleteOrderComment (referenceOrderId, commentId, "deleteReferenceOrderComment", ReferenceOrderTo.class);
    }
    
    private void deleteOrderComment (int orderId, int orderCommentId, String methodName, Class<? extends OrderTo> orderClass) {
    	// validation checks
       	Stream<OrderTo> allDataSources = getAllOrderDataSources();
       	OrderTo orderTo = SharedMockLogic.validateOrderId (this.getClass(), methodName, "orderId", orderId, allDataSources, orderClass);

       	// identify the existing comment
    	List<OrderCommentTo> comments = TestOrderComment.asList().stream().filter(x -> x.id() == orderCommentId).collect(Collectors.toList());
    	if (comments.size () == 0) {
    		comments = CUSTOM_ORDER_COMMENTS.stream().filter(x -> x.id() == orderCommentId).collect(Collectors.toList());
    	}
    	if (comments.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), methodName, "commentId" , "Order Comment", "" + orderCommentId);
    	}
       	
		List<TestReferenceOrder> enumListTestReferenceOrder = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == orderId)
			.collect(Collectors.toList());		
		
		List<TestLimitOrder> enumListTestLimitOrder = TestLimitOrder.asEnumList().stream()
				.filter(x-> x.getEntity().id() == orderId)
				.collect(Collectors.toList());	
		
		Set<Integer> newCommentIds = orderTo.orderCommentIds() != null?new HashSet<>(orderTo.orderCommentIds()):new HashSet<>();
		newCommentIds = newCommentIds.stream().filter(x -> x != orderCommentId).collect(Collectors.toSet());
		
		OrderTo updatedOrder = updateOrderTo (orderTo, newCommentIds);
		
		if (enumListTestReferenceOrder.size() == 1) {
			TestReferenceOrder order = enumListTestReferenceOrder.get(0);
	    	order.setEntity ((ReferenceOrderTo)updatedOrder);
		} else if (enumListTestLimitOrder.size() == 1) {
			TestLimitOrder order = enumListTestLimitOrder.get(0);
	    	order.setEntity ((LimitOrderTo)updatedOrder);
		} else {
			MockOrderController.CUSTOM_ORDERS.remove(orderTo);			
			MockOrderController.CUSTOM_ORDERS.add(updatedOrder);
		}	    	
    }
    
    private int postOrderComment (int orderId, OrderCommentTo newComment, String methodName, Class<? extends OrderTo> orderClass) {
       	Stream<OrderTo> allDataSources = getAllOrderDataSources();
       	OrderTo orderTo = SharedMockLogic.validateOrderId (this.getClass(), methodName, "orderId", orderId, allDataSources, orderClass);
    	// validation checks
    	SharedMockLogic.validateCommentFields (this.getClass(), methodName, "newComment", newComment, true, null);
    	
    	OrderCommentTo withId = ImmutableOrderCommentTo.copyOf(newComment)
    			.withId(ID_COUNTER_ORDER_COMMENT.incrementAndGet());
    	CUSTOM_ORDER_COMMENTS.add (withId);

		List<TestReferenceOrder> enumListTestReferenceOrder = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == orderId)
			.collect(Collectors.toList());		
		
		List<TestLimitOrder> enumListTestLimitOrder = TestLimitOrder.asEnumList().stream()
				.filter(x-> x.getEntity().id() == orderId)
				.collect(Collectors.toList());	
		
		Set<Integer> newCommentIds = orderTo.orderCommentIds() != null?new HashSet<>(orderTo.orderCommentIds()):new HashSet<>();
		newCommentIds.add(withId.id());
		
		OrderTo updatedOrder = updateOrderTo (orderTo, newCommentIds);
		
		if (enumListTestReferenceOrder.size() == 1) {
			TestReferenceOrder order = enumListTestReferenceOrder.get(0);
	    	order.setEntity ((ReferenceOrderTo)updatedOrder);
		} else if (enumListTestLimitOrder.size() == 1) {
			TestLimitOrder order = enumListTestLimitOrder.get(0);
	    	order.setEntity ((LimitOrderTo)updatedOrder);
		} else {
			MockOrderController.CUSTOM_ORDERS.remove(orderTo);			
			MockOrderController.CUSTOM_ORDERS.add(updatedOrder);
		}
		return withId.id();   	    	
    }    

    private OrderTo updateOrderTo ( OrderTo order, Set<Integer> newCommentIds) {
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		if (order instanceof LimitOrderTo) {
			LimitOrderTo updatedLimitOrder = ImmutableLimitOrderTo.copyOf((LimitOrderTo)order)
					.withOrderCommentIds(newCommentIds)
	    			.withLastUpdate(sdfDateTime.format(new Date()));
			return updatedLimitOrder;    				
		} else if (order instanceof ReferenceOrderTo) {
			ReferenceOrderTo updatedReferenceOrder = ImmutableReferenceOrderTo.copyOf((ReferenceOrderTo)order)
					.withOrderCommentIds(newCommentIds)
	    			.withLastUpdate(sdfDateTime.format(new Date()));
			return updatedReferenceOrder;    	
		} else {
			throw new RuntimeException ("Unknown Order type: '" + order.getClass().getName() + "' found while updating order");
		}
    }
    
    private void updateOrderComment (int orderId, int commentId, OrderCommentTo existingComment, String methodName, Class<? extends OrderTo> orderClass) {
		Stream<OrderTo> allDataSources = getAllOrderDataSources();
		OrderTo order = SharedMockLogic.validateOrderId (this.getClass(), methodName, "orderId", orderId, allDataSources, orderClass);

    	if (commentId != existingComment.id()) {
    		throw new IllegalIdException(this.getClass(), methodName, "existingComment/Id",
    				"" + commentId, "" + existingComment.id());
    	}
    	
    	if (order.orderCommentIds() == null || 
    			!order.orderCommentIds().contains(existingComment.id())) {
       		throw new IllegalStateException (this.getClass(), methodName, "commentId", 
       				"The provided comment ID does not match any of the existing comment IDs",
       				"Order #" + commentId,
       				"The existing comment must be one of the following " + order.orderCommentIds());
    	}
    	
       	// identify the existing comment
    	List<OrderCommentTo> comments = TestOrderComment.asList().stream().filter(x -> x.id() == existingComment.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (comments.size () == 0) {
    		comments = CUSTOM_ORDER_COMMENTS.stream().filter(x -> x.id() == existingComment.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (comments.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), methodName, "existingOrderComment.id" , "Order Comment", "" + existingComment.id());
    	}
    	    	
    	SharedMockLogic.validateCommentFields (this.getClass(), methodName, "existingOrderComment", existingComment, false, comments.get(0));
    	// everything passed checks, now update comment data
    	if (isEnum) {
        	List<TestOrderComment> commentEnums = TestOrderComment.asEnumList().stream().filter(x -> x.getEntity().id() == existingComment.id()).collect(Collectors.toList());
        	commentEnums.get(0).setEntity(existingComment);
    	} else {
    		// identification by ID only for comments, following statement is going to remove the existing entry 
    		// having the same ID.
    		CUSTOM_ORDER_COMMENTS.remove(existingComment);
    		CUSTOM_ORDER_COMMENTS.add (existingComment);
    	}    	
    }
    
    private OrderCommentTo getOrderComment (int orderId, int commentId, String methodName, Class<? extends OrderTo> orderClass) {
    	Stream<OrderTo> allDataSources = getAllOrderDataSources();
    	OrderTo order = SharedMockLogic.validateOrderId(this.getClass(), methodName, "orderId", orderId, allDataSources, orderClass);
    	
    	if (order.orderCommentIds() == null) {
    		throw new IllegalIdException(this.getClass(), methodName, "orderId", "<order does not have comments>", "" + orderId);
    	}
	
    	Stream<OrderCommentTo> allDataSourcesComment = Stream.concat(TestOrderComment.asList().stream(), CUSTOM_ORDER_COMMENTS.stream());
    	List<OrderCommentTo> comments = allDataSourcesComment
    			.filter( x -> order.orderCommentIds().contains(x.id()) && x.id() == commentId)
    			.collect(Collectors.toList());
    	if (comments.size() == 1) {
        	return comments.get(0);    		
    	} else {
    		return null;
    	}
    }

	private Stream<OrderTo> getAllOrderDataSources() {
		Stream<OrderTo> allDataSources = Stream.concat(Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream()),
    			TestReferenceOrder.asList().stream());
		return allDataSources;
	}
    
    
    private Set<OrderCommentTo> getOrderComments (int orderId, String methodName, Class<? extends OrderTo> orderClass) {
    	Stream<OrderTo> allDataSources = getAllOrderDataSources();
    	OrderTo orderTo = SharedMockLogic.validateOrderId(this.getClass(), methodName, "limitOrderId", orderId, allDataSources, orderClass);
    	
    	if (orderTo.orderCommentIds() == null) {
    		return null;
    	}
	
    	Stream<OrderCommentTo> allDataSourcesFill = Stream.concat(TestOrderComment.asList().stream(), CUSTOM_ORDER_COMMENTS.stream());
    	Set<OrderCommentTo> comments = allDataSourcesFill
    			.filter( x -> orderTo.orderCommentIds().contains(x.id()))
    			.collect(Collectors.toSet());
    	return comments;    	    	
    }
}
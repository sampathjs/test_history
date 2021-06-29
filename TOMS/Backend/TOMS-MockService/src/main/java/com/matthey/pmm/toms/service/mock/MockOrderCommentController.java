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
    	return  getCommentsOrders (limitOrderId, "getCommentsLimitOrders", LimitOrderTo.class);
    }
    
    @ApiOperation("Creation of a new comment for a Limit Order")
    public int postLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be posted for", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new comment. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment) {
       	Stream<OrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
       	LimitOrderTo limitOrder = SharedMockLogic.validateOrderId (this.getClass(), "postLimitOrderComment", "limitOrderId", limitOrderId, allDataSources, LimitOrderTo.class);
    	// validation checks
    	SharedMockLogic.validateCommentFields (this.getClass(), "postLimitOrderComment", "newComment", newComment, true, null);

    	OrderCommentTo withId = ImmutableOrderCommentTo.copyOf(newComment)
    			.withId(ID_COUNTER_ORDER_COMMENT.incrementAndGet());
    	CUSTOM_ORDER_COMMENTS.add (withId);

		List<TestLimitOrder> enumList = TestLimitOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == limitOrderId)
			.collect(Collectors.toList());
		
		Set<Integer> newCommentIds = limitOrder.orderCommentIds() != null?new HashSet<>(limitOrder.orderCommentIds()):new HashSet<>();
		newCommentIds.add(withId.id());
		
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		LimitOrderTo updatedLimitOrder = ImmutableLimitOrderTo.copyOf(limitOrder)
				.withOrderCommentIds(newCommentIds)
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

    @ApiOperation("Update of a comment for a Limit Order")
    public void updateLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1") @PathVariable int commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment) {
		Stream<OrderTo> allDataSources = Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
		LimitOrderTo limitOrder = SharedMockLogic.validateOrderId (this.getClass(), "updateLimitOrderComment", "limitOrderId", limitOrderId, allDataSources, LimitOrderTo.class);

    	if (commentId != existingComment.id()) {
    		throw new IllegalIdException(this.getClass(), "updateLimitOrderComment", "existingComment/Id",
    				"" + commentId, "" + existingComment.id());
    	}
    	
    	if (limitOrder.orderCommentIds() == null || 
    			!limitOrder.orderCommentIds().contains(existingComment.id())) {
       		throw new IllegalStateException (this.getClass(), "updateLimitOrderComment", "commentId", 
       				"The provided comment ID does not match any of the existing comment IDs",
       				"Order #" + commentId,
       				"The existing comment must be one of the following " + limitOrder.orderCommentIds());
    	}
    	
       	// identify the existing comment
    	List<OrderCommentTo> comments = TestOrderComment.asList().stream().filter(x -> x.id() == existingComment.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (comments.size () == 0) {
    		comments = CUSTOM_ORDER_COMMENTS.stream().filter(x -> x.id() == existingComment.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (comments.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateLimitOrderComment", "existingOrderComment.id" , "Order Comment", "" + existingComment.id());
    	}
    	    	
    	SharedMockLogic.validateCommentFields (this.getClass(), "updateLimitOrderComment", "existingOrderComment", existingComment, false, comments.get(0));
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

    
    @ApiOperation("Retrieval of a comment  for a Limit Order")
    public OrderCommentTo getCommentLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the comment is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1") @PathVariable int commentId) {
    	return getCommentOrder (limitOrderId, commentId, "getCommentLimitOrder", LimitOrderTo.class);
    }
    
    @ApiOperation("Retrieval of the comment data for a reference Order")
    public Set<OrderCommentTo> getCommentsReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the comments are to be retrieved from", example = "1001") @PathVariable int referenceOrderId) {
    	return getCommentsOrders (referenceOrderId, "getCommentsReferenceOrders", ReferenceOrderTo.class);
    }

    @ApiOperation("Retrieval of a comment  for a reference Order")
    public OrderCommentTo getCommentReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the comment is to be retrieved from", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1") @PathVariable int commentId) {
    	return getCommentOrder (referenceOrderId, commentId, "getCommentReferenceOrder", ReferenceOrderTo.class);
    }

    @ApiOperation("Creation of a new comment for a reference Order")
    public int postReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be posted for", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new comment. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment) {
       	Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
       	ReferenceOrderTo referenceOrderTo = SharedMockLogic.validateOrderId (this.getClass(), "postReferenceOrderComment", "referenceOrderId", referenceOrderId, allDataSources, ReferenceOrderTo.class);
    	// validation checks
    	SharedMockLogic.validateCommentFields (this.getClass(), "postReferenceOrderComment", "newComment", newComment, true, null);

    	OrderCommentTo withId = ImmutableOrderCommentTo.copyOf(newComment)
    			.withId(ID_COUNTER_ORDER_COMMENT.incrementAndGet());
    	CUSTOM_ORDER_COMMENTS.add (withId);

		List<TestReferenceOrder> enumList = TestReferenceOrder.asEnumList().stream()
			.filter(x-> x.getEntity().id() == referenceOrderId)
			.collect(Collectors.toList());
		
		
		
		Set<Integer> newCommentIds = referenceOrderTo.orderCommentIds() != null?new HashSet<>(referenceOrderTo.orderCommentIds()):new HashSet<>();
		newCommentIds.add(withId.id());
		
		SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		ReferenceOrderTo updatedReferenceOrder = ImmutableReferenceOrderTo.copyOf(referenceOrderTo)
				.withOrderCommentIds(newCommentIds)
    			.withLastUpdate(sdfDateTime.format(new Date()));

		if (enumList.size() == 1) {
			TestReferenceOrder order = enumList.get(0);
	    	order.setEntity (updatedReferenceOrder);
		} else {
			MockOrderController.CUSTOM_ORDERS.remove(referenceOrderTo);			
			MockOrderController.CUSTOM_ORDERS.add(updatedReferenceOrder);
		}
		return withId.id();   	
    }

    @ApiOperation("Update of a comment for a reference Order")
    public void updateReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be updated for ", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1001") @PathVariable int commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment) {
		Stream<OrderTo> allDataSources = Stream.concat(TestReferenceOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream());
		ReferenceOrderTo referenceOrder = SharedMockLogic.validateOrderId (this.getClass(), "updateReferenceOrderComment", "referenceOrderId", referenceOrderId, allDataSources, ReferenceOrderTo.class);

    	if (commentId != existingComment.id()) {
    		throw new IllegalIdException(this.getClass(), "updateReferenceOrderComment", "existingComment/Id",
    				"" + commentId, "" + existingComment.id());
    	}
    	
    	if (referenceOrder.orderCommentIds() == null || 
    			!referenceOrder.orderCommentIds().contains(existingComment.id())) {
       		throw new IllegalStateException (this.getClass(), "updateReferenceOrderComment", "commentId", 
       				"The provided comment ID does not match any of the existing comment IDs",
       				"Order #" + commentId,
       				"The existing comment must be one of the following " + referenceOrder.orderCommentIds());
    	}
    	
       	// identify the existing comment
    	List<OrderCommentTo> comments = TestOrderComment.asList().stream().filter(x -> x.id() == existingComment.id()).collect(Collectors.toList());
    	boolean isEnum = true;
    	if (comments.size () == 0) {
    		comments = CUSTOM_ORDER_COMMENTS.stream().filter(x -> x.id() == existingComment.id()).collect(Collectors.toList());
    		isEnum = false;
    	}
    	if (comments.size() == 0) {
    		throw new UnknownEntityException (this.getClass(), "updateReferenceOrderComment", "existingOrderComment.id" , "Order Comment", "" + existingComment.id());
    	}
    	    	
    	SharedMockLogic.validateCommentFields (this.getClass(), "updateReferenceOrderComment", "existingOrderComment", existingComment, false, comments.get(0));
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

    
    private OrderCommentTo getCommentOrder (int orderId, int commentId, String methodName, Class<? extends OrderTo> orderClass) {
    	Stream<OrderTo> allDataSources = Stream.concat(Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream()),
    			TestReferenceOrder.asList().stream());
    	LimitOrderTo limitOrder = SharedMockLogic.validateOrderId(this.getClass(), methodName, "orderId", orderId, allDataSources, LimitOrderTo.class);
    	
    	if (limitOrder.orderCommentIds() == null) {
    		throw new IllegalIdException(this.getClass(), methodName, "orderId", "<order does not have comments>", "" + orderId);
    	}
	
    	Stream<OrderCommentTo> allDataSourcesComment = Stream.concat(TestOrderComment.asList().stream(), CUSTOM_ORDER_COMMENTS.stream());
    	List<OrderCommentTo> comments = allDataSourcesComment
    			.filter( x -> limitOrder.orderCommentIds().contains(x.id()) && x.id() == commentId)
    			.collect(Collectors.toList());
    	if (comments.size() == 1) {
        	return comments.get(0);    		
    	} else {
    		return null;
    	}
    }
    
    
    private Set<OrderCommentTo> getCommentsOrders (int orderId, String methodName, Class<? extends OrderTo> orderClass) {
    	Stream<OrderTo> allDataSources = Stream.concat(Stream.concat(TestLimitOrder.asList().stream(), MockOrderController.CUSTOM_ORDERS.stream()),
    			TestReferenceOrder.asList().stream());
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
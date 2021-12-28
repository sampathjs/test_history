package com.matthey.pmm.toms.service.impl;


import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsOrderCommentService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Transactional
public abstract class OrderCommentControllerImpl implements TomsOrderCommentService {
	@Autowired
	protected TomsValidator validator;
	
	@Autowired
	protected OrderCommentRepository orderCommentRepo;
	
	@Autowired
	protected OrderCommentConverter orderCommentConverter;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository referenceOrderRepo;
	
	@Autowired
	protected ReferenceConverter referenceConverter;
	    
    @ApiOperation("Retrieval of the comment data for a Limit Order")
    public Set<OrderCommentTo> getCommentsLimitOrders (
    		@ApiParam(value = "The order ID of the limit order the comments are to be retrieved from", example = "100000") @PathVariable long limitOrderId) {
    	return  getOrderComments (limitOrderId, "getCommentsLimitOrders", LimitOrderTo.class);
    }
    
    @ApiOperation("Creation of a new comment for a Limit Order")
    public long postLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be posted for", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The new comment. ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment) {
    	return postOrderComment (limitOrderId, newComment, "postLimitOrderComment", LimitOrderTo.class);
    }

    @ApiOperation("Update of a comment for a Limit Order")
    public void updateLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1000000") @PathVariable long commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment) {
    	updateOrderComment (limitOrderId, commentId, existingComment, "updateLimitOrderComment", LimitOrderTo.class);
    }
    
    @ApiOperation("Retrieval of a comment  for a Limit Order")
    public OrderCommentTo getCommentLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the comment is to be retrieved from", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1000000") @PathVariable long commentId) {
    	return getOrderComment (limitOrderId, commentId, "getCommentLimitOrder", LimitOrderTo.class);
    }
    
    @ApiOperation("Deletion of a comment for a Limit Order")
    public void deleteLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be deleted for ", example = "100000") @PathVariable long limitOrderId,
    		@ApiParam(value = "The ID of the comment to delete ", example = "1000000") @PathVariable long commentId) {
    	deleteOrderComment (limitOrderId, commentId, "deleteLimitOrder", LimitOrderTo.class);
    }
    
    @ApiOperation("Retrieval of the comment data for a reference Order")
    public Set<OrderCommentTo> getCommentsReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the comments are to be retrieved from", example = "100003") @PathVariable long referenceOrderId) {
    	return getOrderComments (referenceOrderId, "getCommentsReferenceOrders", ReferenceOrderTo.class);
    }

    @ApiOperation("Retrieval of a comment  for a reference Order")
    public OrderCommentTo getCommentReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the comment is to be retrieved from", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1000003") @PathVariable long commentId) {
    	return getOrderComment (referenceOrderId, commentId, "getCommentReferenceOrder", ReferenceOrderTo.class);
    }

    @ApiOperation("Creation of a new comment for a reference Order")
    public long postReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be posted for", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The new comment. ID has to be 0. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment) {
    	return postOrderComment (referenceOrderId, newComment, "postReferenceOrderComment", ReferenceOrderTo.class);
    }

    @ApiOperation("Update of a comment for a reference Order")
    public void updateReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be updated for ", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1000003") @PathVariable long commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment) {
    	updateOrderComment (referenceOrderId, commentId, existingComment, "updateReferenceOrderComment", ReferenceOrderTo.class);
    }
    
    @ApiOperation("Deletion of a comment for a Reference Order")
    public void deleteReferenceOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "100003") @PathVariable long referenceOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1000003") @PathVariable long commentId) {
    	deleteOrderComment (referenceOrderId, commentId, "deleteReferenceOrderComment", ReferenceOrderTo.class);
    }
    
	@Transactional
    private void deleteOrderComment (long orderId, long orderCommentId, String methodName, Class<? extends OrderTo> orderClass) {
    	Order order;
    	String orderIdArgumentName;
    	if (orderClass == LimitOrderTo.class) {
    		orderIdArgumentName = "limitOrderId";
    		validator.verifyLimitOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false);
    		order = limitOrderRepo.findLatestByOrderId(orderId).get();
    	} else {
    		orderIdArgumentName = "referenceOrderId";
    		validator.verifyReferenceOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false);
    		order = referenceOrderRepo.findLatestByOrderId(orderId).get();   		
    	}
    	
    	Optional<OrderComment> existingOrderComment = validator.verifyOrderCommentId (orderCommentId, this.getClass(), methodName, "orderCommentId", false);   	
       	validator.verifyOrderCommentBelongsToOrder (existingOrderComment.get(), order, this.getClass(), methodName, "Order", "Order Comment");

    	existingOrderComment.get().setLifecycleStatus(referenceConverter.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_DELETED.getEntity()));
    	orderCommentRepo.save(existingOrderComment.get());
    	order.setVersion(order.getVersion() + 1);
    	if (orderClass == LimitOrderTo.class) {
    		limitOrderRepo.save((LimitOrder)order);
    	} else {
    		referenceOrderRepo.save((ReferenceOrder)order);    		
    	}
    	
    }
    
	@Transactional
    private long postOrderComment (long orderId, OrderCommentTo newComment, String methodName, Class<? extends OrderTo> orderClass) {
    	Order order;
    	String orderIdArgumentName;
    	if (orderClass == LimitOrderTo.class) {
    		orderIdArgumentName = "limitOrderId";
    		order = validator.verifyLimitOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false).get();
    	} else {
    		orderIdArgumentName = "referenceOrderId";
    		order = validator.verifyReferenceOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false).get();
    	}   	
    	validator.validateCommentFields(getClass(), methodName, orderIdArgumentName, newComment, order, true, null);
 
    	OrderComment mangedEntity = orderCommentConverter.toManagedEntity(newComment); // persists comment    	   	
    	order.getOrderComments().add(mangedEntity);
    	order.setUpdatedByUser(mangedEntity.getUpdatedByUser());
    	order.setLastUpdate(new Date());
    	order.setVersion(order.getVersion()+1);

    	if (orderClass == LimitOrderTo.class) {
    		limitOrderRepo.save((LimitOrder)order);
    	} else {
    		referenceOrderRepo.save((ReferenceOrder)order);
    	}   	
    	return mangedEntity.getId();
    }    

	@Transactional
    private void updateOrderComment (long orderId, long commentId, OrderCommentTo existingComment, String methodName, Class<? extends OrderTo> orderClass) {
    	Order order;
    	String orderIdArgumentName;
    	if (orderClass == LimitOrderTo.class) {
    		orderIdArgumentName = "limitOrderId";
    		order = validator.verifyLimitOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false).get();
    	} else {
    		orderIdArgumentName = "referenceOrderId";
    		order = validator.verifyReferenceOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false).get();
    	} 	
    	
    	Optional<OrderComment> oldOrderCommentedInDb = validator.verifyOrderCommentId(commentId, getClass(), methodName, "existingComment", false);
    	validator.verifyOrderCommentBelongsToOrder(oldOrderCommentedInDb.get(), order, getClass(), methodName, "Order", "Order Comment");
    	validator.validateCommentFields(getClass(), methodName, "existingComment", existingComment, order, false, oldOrderCommentedInDb.get());

    	OrderComment updated = orderCommentConverter.toManagedEntity(existingComment);
    	orderCommentRepo.save(updated);
    	if (orderClass == LimitOrderTo.class) {
    		order.setVersion(order.getVersion()+1);
    		limitOrderRepo.save((LimitOrder)order);
    	} else {
    		order.setVersion(order.getVersion()+1);
    		referenceOrderRepo.save((ReferenceOrder)order);
    	} 
    }
    
	@Transactional
    private OrderCommentTo getOrderComment (long orderId, long commentId, String methodName, Class<? extends OrderTo> orderClass) {
    	Order order;
    	String orderIdArgumentName;
    	if (orderClass == LimitOrderTo.class) {
    		orderIdArgumentName = "limitOrderId";
    		validator.verifyLimitOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false);
    		order = limitOrderRepo.findLatestByOrderId(orderId).get();
    	} else {
    		orderIdArgumentName = "referenceOrderId";
    		order = validator.verifyReferenceOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false).get();
    	} 	
    	
    	if (order.getOrderComments() == null || order.getOrderComments().size() == 0) {
    		throw new IllegalIdException(this.getClass(), methodName, "orderId", "<order does not have comments>", "" + orderId);
    	}
    	Optional<OrderComment> orderComment = validator.verifyOrderCommentId(commentId, getClass(), methodName, "orderCommentId", false);
    	validator.verifyOrderCommentBelongsToOrder(orderComment.get(), order, getClass(), methodName, "Order", "Order Comment");
    	return orderCommentConverter.toTo(orderComment.get());
    }    
    
	@Transactional
    private Set<OrderCommentTo> getOrderComments (long orderId, String methodName, Class<? extends OrderTo> orderClass) {
    	Order order;
    	String orderIdArgumentName;
    	if (orderClass == LimitOrderTo.class) {
    		orderIdArgumentName = "limitOrderId";
    		validator.verifyLimitOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false);
    		order = limitOrderRepo.findLatestByOrderId(orderId).get();
    	} else {
    		orderIdArgumentName = "referenceOrderId";
    		validator.verifyReferenceOrderId(orderId, this.getClass(), methodName, orderIdArgumentName, false);
    		order = referenceOrderRepo.findLatestByOrderId(orderId).get();   		
    	}
    	
    	if (order.getOrderComments() == null) {
    		return null;
    	}
	
    	return order.getOrderComments().stream()
    			.filter (x -> x.getLifecycleStatus().getId() != DefaultReference.LIFECYCLE_STATUS_DELETED.getEntity().id())
    			.map(x -> orderCommentConverter.toTo(x))
    			.collect(Collectors.toSet());
    }
}
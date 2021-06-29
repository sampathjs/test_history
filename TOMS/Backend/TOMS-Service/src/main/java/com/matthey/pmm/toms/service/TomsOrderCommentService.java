package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.matthey.pmm.toms.transport.OrderCommentTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Order Comments Related Data"}, description = "APIs for Order Comments")
@RequestMapping(API_PREFIX)
public interface TomsOrderCommentService {    
    @Cacheable({"CommentLimitOrder"})
    @ApiOperation("Retrieval of the comment data for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/comments/")
    public Set<OrderCommentTo> getCommentsLimitOrders (
    		@ApiParam(value = "The order ID of the limit order the comments are to be retrieved from", example = "1") @PathVariable int limitOrderId);

    @Cacheable({"CommentLimitOrder"})
    @ApiOperation("Retrieval of a comment  for a Limit Order")
	@GetMapping("/limitOrder/{limitOrderId}/comments/{commentId}")
    public OrderCommentTo getCommentLimitOrder (
    		@ApiParam(value = "The order ID of the limit order the comment is to be retrieved from", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1") @PathVariable int commentId);

    @ApiOperation("Creation of a new comment for a Limit Order")
	@PostMapping("/limitOrder/{limitOrderId}/comments")    
    public int postLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be posted for", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The new comment. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment);

    @ApiOperation("Update of a comment for a Limit Order")
	@PutMapping("/limitOrder/{limitOrderId}/comments/{commentId}")    
    public void updateLimitOrderComment (
    		@ApiParam(value = "The order ID of the limit order the comment is to be updated for ", example = "1") @PathVariable int limitOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1") @PathVariable int commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment);
    
    @Cacheable({"CommentreferenceOrder"})
    @ApiOperation("Retrieval of the comment data for a reference Order")
	@GetMapping("/referenceOrder/{referenceOrderId}/comments/")
    public Set<OrderCommentTo> getCommentsReferenceOrders (
    		@ApiParam(value = "The order ID of the reference order the comments are to be retrieved from", example = "1001") @PathVariable int referenceOrderId);

    @Cacheable({"CommentreferenceOrder"})
    @ApiOperation("Retrieval of a comment  for a reference Order")
	@GetMapping("/referenceOrder/{referenceOrderId}/comments/{commentId}")
    public OrderCommentTo getCommentReferenceOrder (
    		@ApiParam(value = "The order ID of the reference order the comment is to be retrieved from", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to retrieve ", example = "1") @PathVariable int commentId);

    @ApiOperation("Creation of a new comment for a reference Order")
	@PostMapping("/referenceOrder/{referenceOrderId}/comments")    
    public int postReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be posted for", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The new comment. ID has to be -1. The actual assigned ID is going to be returned", example = "", required = true) @RequestBody(required=true) OrderCommentTo newComment);

    @ApiOperation("Update of a comment for a reference Order")
	@PutMapping("/referenceOrder/{referenceOrderId}/comments/{commentId}")    
    public void updateReferenceOrderComment (
    		@ApiParam(value = "The order ID of the reference order the comment is to be updated for ", example = "1001") @PathVariable int referenceOrderId,
    		@ApiParam(value = "The ID of the comment to update ", example = "1") @PathVariable int commentId,
    		@ApiParam(value = "The updated comment. ID has to be matching the ID of the existing comment.", example = "", required = true) @RequestBody(required=true) OrderCommentTo existingComment);
}

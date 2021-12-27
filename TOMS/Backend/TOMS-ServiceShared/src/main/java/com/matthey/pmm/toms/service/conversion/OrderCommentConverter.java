package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.OrderComment;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.ImmutableOrderCommentTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;

@Service
public class OrderCommentConverter extends EntityToConverter<OrderComment, OrderCommentTo>{
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private OrderCommentRepository entityRepo;
	
	@Autowired
	private ReferenceRepository refRepo;
	
	@Override
	public UserRepository userRepo() {
		return userRepo;
	}
	
	@Override
	public ReferenceRepository refRepo () {
		return refRepo;
	}
	
	
	@Override
	public OrderCommentTo toTo (OrderComment entity) {
		return ImmutableOrderCommentTo.builder()
				.lastUpdate(super.formatDateTime(entity.getLastUpdate()))
				.createdAt(super.formatDateTime(entity.getCreatedAt()))
				.commentText(entity.getCommentText())
				.id(entity.getId())
				.idCreatedByUser(entity.getCreatedByUser().getId())
				.idUpdatedByUser(entity.getUpdatedByUser().getId())
				.displayStringCreatedByUser(entity.getCreatedByUser() != null?entity.getCreatedByUser().getLastName():null)
				.displayStringUpdatedByUser(entity.getUpdatedByUser() != null?entity.getUpdatedByUser().getLastName():null)
				.idLifeCycle(entity.getLifecycleStatus().getId())
				.build();
	}
	
	@Override
	@Transactional
	public OrderComment toManagedEntity (OrderCommentTo to) {		
		User createdBy = loadUser(to, to.idCreatedByUser());
		User updatedBy = loadUser(to, to.idUpdatedByUser());
		Reference lifeCycleStatus = loadRef(to, to.idLifeCycle());
		Optional<OrderComment> existingOrderComment = entityRepo.findById(to.id());
		
		if (existingOrderComment.isPresent()) {
			existingOrderComment.get().setCommentText(to.commentText());
			existingOrderComment.get().setCreatedAt(parseDateTime(to, to.createdAt()));
			existingOrderComment.get().setLastUpdate(parseDateTime(to, to.lastUpdate()));
			existingOrderComment.get().setCreatedByUser(createdBy);
			existingOrderComment.get().setUpdatedByUser(updatedBy);
			existingOrderComment.get().setLifecycleStatus(lifeCycleStatus);
			return existingOrderComment.get();
		}
		OrderComment newOrderComment =  new OrderComment(to.commentText(), parseDateTime(to, to.createdAt()), 
				createdBy, parseDateTime(to, to.lastUpdate()), updatedBy, lifeCycleStatus);
		newOrderComment = entityRepo.save(newOrderComment);
		return newOrderComment;
	}
}

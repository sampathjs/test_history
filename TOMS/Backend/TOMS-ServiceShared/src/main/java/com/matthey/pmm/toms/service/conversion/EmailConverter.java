package com.matthey.pmm.toms.service.conversion;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.DatabaseFile;
import com.matthey.pmm.toms.model.Email;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.repository.EmailRepository;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.ImmutableEmailTo;

@Service
public class EmailConverter extends EntityToConverter<Email, EmailTo>{
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private EmailRepository entityRepo;
	
	@Autowired
	private DatabaseFileRepository databaseFileRepo;
	
	@Autowired
	private OrderRepository orderRepo;	

	@Autowired
	private ReferenceRepository refRepo;	
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public UserRepository userRepo() {
		return userRepo;
	}
	
	@Override
	public DatabaseFileRepository databaseFileRepo() {
		return databaseFileRepo;
	}
	
	@Override
	public OrderRepository orderRepo() {
		return orderRepo;
	}
	
	@Override
	public EmailTo toTo (Email entity) {
		return ImmutableEmailTo.builder()
				.associatedOrderIds(entity.getAssociatedOrders())
				.attachments(entity.getAttachments() != null?entity.getAttachments().stream().map(x -> x.getId()).collect(Collectors.toSet()):null)
				.bccList(entity.getBccSet())
				.body(entity.getBody())
				.ccList(entity.getCcSet())
				.createdAt(formatDateTime(entity.getCreatedAt()))
				.errorMessage(entity.getErrorMessage())
				.lastUpdate(formatDateTime(entity.getLastUpdate()))
				.id(entity.getId())
				.idCreatedByUser(entity.getCreatedByUser().getId())
				.idEmailStatus(entity.getEmailStatus().getId())
				.idSendAs(entity.getSendAs().getId())
				.idUpdatedByUser(entity.getUpdatedByUser().getId())
				.retryCount(entity.getRetryCount())
				.subject(entity.getSubject())
				.toList(entity.getToSet())
				.build();
	}
	
	@Override
	public Email toManagedEntity (EmailTo to) {
		Set<Order> associatedOrders = to.associatedOrderIds() != null?
				new HashSet<>(to.associatedOrderIds().stream().map(x -> loadOrder(to, x)).collect(Collectors.toSet())):null;
		Set<DatabaseFile> attachments = to.attachments() != null?
				new HashSet<>(to.attachments().stream().map(x -> loadDatabaseFile(to, x)).collect(Collectors.toSet())):null;
		User createdBy = loadUser(to, to.idCreatedByUser());
		Reference emailStatus = loadRef(to, to.idEmailStatus());
		User sendAs = loadUser(to, to.idSendAs());
		User updatedBy = loadUser(to, to.idUpdatedByUser());
		Optional<Email> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			if (   !existingEntity.get().getAssociatedOrders().containsAll(associatedOrders)
				|| !associatedOrders.containsAll(existingEntity.get().getAssociatedOrders())) {
				existingEntity.get().setAssociatedOrders(associatedOrders);				
			}
			existingEntity.get().setAttachments(attachments);
			existingEntity.get().setBccSet(new HashSet<>(to.bccList()));
			existingEntity.get().setBody(to.body());
			existingEntity.get().setCcSet(new HashSet<>(to.ccList()));
			existingEntity.get().setCreatedAt(parseDateTime(to, to.createdAt()));
			existingEntity.get().setCreatedByUser(createdBy);
			existingEntity.get().setEmailStatus(emailStatus);
			existingEntity.get().setErrorMessage(to.errorMessage());
			existingEntity.get().setLastUpdate(parseDateTime(to, to.lastUpdate()));
			existingEntity.get().setRetryCount(to.retryCount());
			existingEntity.get().setSendAs(sendAs);
			existingEntity.get().setSubject(to.subject());
			existingEntity.get().setToSet(new HashSet<>(to.toList()));
			existingEntity.get().setUpdatedByUser(updatedBy);
			return existingEntity.get();
		}
		Email newEntity =  new Email(sendAs,to.subject(), to.body(), to.toList(), to.ccList(), to.bccList(), attachments, emailStatus, to.errorMessage(), to.retryCount(), 
				parseDateTime(to, to.createdAt()), createdBy, parseDateTime(to, to.lastUpdate()), updatedBy, associatedOrders);
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

package com.matthey.pmm.toms.service.conversion;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.matthey.pmm.toms.model.ProcessTransition;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.ProcessTransitionRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.ImmutableProcessTransitionTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;

public class ProcessTransitionConverter extends EntityToConverter<ProcessTransition, ProcessTransitionTo> {
	@Autowired
	private ReferenceTypeRepository refTypeRepo;

	@Autowired
	private ReferenceRepository refRepo;
	
	@Autowired
	private ProcessTransitionRepository entityRepo;

	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		return refTypeRepo;
	}

	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}

	@Override
	public ProcessTransitionTo toTo(ProcessTransition entity) {
		return ImmutableProcessTransitionTo.builder()
				.unchangeableAttributes(entity.getUnchangeableAttributes())
				.toStatusId(entity.getToStatusId())
				.referenceCategoryId(entity.getReferenceCategory().getId())
				.id(entity.getId())
				.fromStatusId(entity.getFromStatusId())
				.build();
	}

	@Override
	public ProcessTransition toManagedEntity(ProcessTransitionTo to) {
		Reference referenceCategory = loadRef(to, to.id());		
		Optional<ProcessTransition> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setUnchangeableAttributes(new ArrayList<>(to.unchangeableAttributes()));
			existingEntity.get().setToStatusId(to.toStatusId());
			existingEntity.get().setReferenceCategory(referenceCategory);
			existingEntity.get().setFromStatusId(to.fromStatusId());
			return existingEntity.get();
		} 
		ProcessTransition newEntity = new ProcessTransition(referenceCategory, to.fromStatusId(), to.toStatusId(), 
				new ArrayList<>(to.unchangeableAttributes()));
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}

}

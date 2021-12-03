package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

@Service
public class ReferenceConverter extends EntityToConverter<Reference, ReferenceTo> {
	@Autowired
	private ReferenceRepository entityRepo;

	@Autowired
	private ReferenceTypeRepository refTypeRepo;
	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		return refTypeRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return entityRepo;
	}
	
	@Override
	public ReferenceTo toTo (Reference entity) {
		return ImmutableReferenceTo.builder()
				.id(entity.getId())
				.name(entity.getValue())
				.endurId(entity.getEndurId())
				.displayName(entity.getDisplayName())
				.idType(entity.getType().getId())
				.idLifecycle(entity.getLifecycleStatus() != null?entity.getLifecycleStatus().getId():null)
				.sortColumn(entity.getSortColumn())
				.build();
	}
	
	@Override
	public Reference toManagedEntity (ReferenceTo to) {		
		ReferenceType type = loadRefType(to, to.idType());
		Reference lifecycleStatus = to.idLifecycle() != null?loadRef (to, to.idLifecycle()):null;
		Optional<Reference> entity = entityRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setEndurId(to.endurId());
			entity.get().setType(type);
			entity.get().setValue(to.name());
			entity.get().setLifecycleStatus(lifecycleStatus);
			entity.get().setSortColumn(to.sortColumn());
			return entity.get();
		}
		Reference newEntity = new Reference (type, to.name(), to.displayName(), to.endurId(), lifecycleStatus, to.sortColumn());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

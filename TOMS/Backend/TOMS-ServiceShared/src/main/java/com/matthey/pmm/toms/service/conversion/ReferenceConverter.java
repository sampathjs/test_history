package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

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
	
	public Reference toEntity (ReferenceTo to) {		
		ReferenceTypeTo typeTo =  DefaultReferenceType.asList()
				.stream()
				.filter(x -> x.id() == to.idType())
				.collect(Collectors.toList())
				.get(0);
		ReferenceType type = new ReferenceTypeConverter().toEntity(typeTo);
		Reference entity = new Reference (type, to.name(), to.displayName(), to.endurId());
		return entity;
	}
	
	@Override
	public ReferenceTo toTo (Reference entity) {
		return ImmutableReferenceTo.builder()
				.id(entity.getId())
				.endurId(entity.getEndurId())
				.displayName(entity.getDisplayName())
				.idType(entity.getType().getId())
				.build();
	}
	
	@Override
	public Reference toManagedEntity (ReferenceTo to) {		
		ReferenceType type = loadRefType(to, to.idType());
		Optional<Reference> entity = entityRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setDisplayName(to.displayName());
			entity.get().setEndurId(to.endurId());
			entity.get().setType(type);
			entity.get().setValue(to.name());
			return entity.get();
		}
		Reference newEntity = new Reference (type, to.name(), to.displayName(), to.endurId());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

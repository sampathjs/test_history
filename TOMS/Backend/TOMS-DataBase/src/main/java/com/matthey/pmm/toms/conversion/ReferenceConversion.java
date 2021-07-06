package com.matthey.pmm.toms.conversion;

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
public class ReferenceConversion {
	@Autowired
	private ReferenceRepository refRepo;

	@Autowired
	private ReferenceTypeRepository refTypeRepo;
	
	public Reference toEntity (ReferenceTo to) {		
		ReferenceTypeTo typeTo =  DefaultReferenceType.asList()
				.stream()
				.filter(x -> x.id() == to.idType())
				.collect(Collectors.toList())
				.get(0);
		ReferenceType type = new ReferenceTypeConversion().toEntity(typeTo);
		Reference entity = new Reference (type, to.name(), to.displayName(), to.endurId());
		return entity;
	}
	
	public ReferenceTo toTo (Reference entity) {
		return ImmutableReferenceTo.builder()
				.id(entity.getId())
				.endurId(entity.getEndurId())
				.displayName(entity.getDisplayName())
				.idType(entity.getType().getId())
				.build();
	}
	
	public Reference toManagedEntity (ReferenceTo to) {		
		Optional<ReferenceType> type = refTypeRepo.findById(to.idType());
		Optional<Reference> entity = refRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setDisplayName(to.displayName());
			entity.get().setEndurId(to.endurId());
			entity.get().setType(type.get());
			entity.get().setValue(to.name());
			return entity.get();
		}
		Reference newEntity = new Reference (type.get(), to.name(), to.displayName(), to.endurId());
		newEntity = refRepo.save(newEntity);
		return newEntity;
	}
}

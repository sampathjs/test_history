package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.ImmutableReferenceTypeTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

@Service
public class ReferenceTypeConverter extends EntityToConverter<ReferenceType, ReferenceTypeTo>{
	@Autowired
	private ReferenceTypeRepository refTypeRepo;

	@Override
	public ReferenceTypeTo toTo (ReferenceType entity) {
		return ImmutableReferenceTypeTo.builder()
				.id(entity.getId())
				.name(entity.getName())
				.build();
	}
	
	/**
	 * Retrieved a JPA managed instance of the TO. If necessary it is going to persist
	 * the entity first to the database to retrieve an entity managed by JPA.
	 * It does not take over the ID provided in the TO, but returns a new entity with a new 
	 * ID in case the entity does not exist on the database yet.
	 * @param refTypeRepo The repository to use
	 * @param to the TO to use.
	 * @return
	 */
	@Override
	@Transactional
	public ReferenceType toManagedEntity (ReferenceTypeTo to) {		
		Optional<ReferenceType> entity = refTypeRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setName(to.name());
			entity.get().setSortColumn(to.sortColumn());
			return entity.get();
		}
		ReferenceType newEntity = new ReferenceType (to.name(), to.sortColumn());
		newEntity = refTypeRepo.save(newEntity);
		return newEntity;
	}
}

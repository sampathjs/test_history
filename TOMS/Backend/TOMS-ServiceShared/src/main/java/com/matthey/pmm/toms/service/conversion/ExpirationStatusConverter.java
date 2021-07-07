package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.ExpirationStatus;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.ExpirationStatusRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.ExpirationStatusTo;
import com.matthey.pmm.toms.transport.ImmutableExpirationStatusTo;

@Service
public class ExpirationStatusConverter extends EntityToConverter<ExpirationStatus, ExpirationStatusTo> {
	@Autowired
	private ReferenceRepository refRepo;
	
	@Autowired
	private ReferenceTypeRepository refTypeRepo;

	@Autowired
	private ExpirationStatusRepository entityRepo;

	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		return refTypeRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public ExpirationStatusTo toTo(ExpirationStatus entity) {
		return ImmutableExpirationStatusTo.builder()
				.id(entity.getId())
				.idExpirationStatusName(entity.getExpirationStatusName().getId())
				.idOrderTypeName(entity.getOrderType().getId())
				.build();
	}

	@Override
	public ExpirationStatus toManagedEntity(ExpirationStatusTo to) {
		Reference expirationStatus = loadRef(to, to.idExpirationStatusName());
		Reference orderTypeName = loadRef(to, to.idOrderTypeName());
		
		Optional<ExpirationStatus> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setExpirationStatusName(expirationStatus);
			existingEntity.get().setOrderType(orderTypeName);
			return existingEntity.get();
		} 
		ExpirationStatus newEntity = new ExpirationStatus(expirationStatus, orderTypeName);
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

package com.matthey.pmm.toms.service.conversion;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.AttributeCalculation;
import com.matthey.pmm.toms.repository.AttributeCalculationRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.ImmutableAttributeCalculationTo;

@Service
public class AttributeCalculationConverter extends EntityToConverter<AttributeCalculation, AttributeCalculationTo> {
	@Autowired
	AttributeCalculationRepository entityRepo;
	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		// not using the refType for conversion in this class.
		return null;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		// not using the reference for conversion in this class.
		return null;
	}

	
	@Override
	public AttributeCalculationTo toTo(AttributeCalculation entity) {
		return ImmutableAttributeCalculationTo.builder()
				.id(entity.getId())
				.addAllDependentAttributes(entity.getDependentAttributes())
				.className(entity.getClassName())
				.spelExpression(entity.getSpelExpression())
				.build();
	}

	@Override
	public AttributeCalculation toManagedEntity(AttributeCalculationTo to) {
		Optional<AttributeCalculation> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setAttributeName(to.attributeName());
			existingEntity.get().setClassName(to.className());
			existingEntity.get().setDependentAttributes(new ArrayList<>(to.dependentAttributes()));
			existingEntity.get().setSpelExpression(to.spelExpression());
			return existingEntity.get();
		}
		AttributeCalculation newEntity = new AttributeCalculation(to.className(), 
				to.dependentAttributes(), to.attributeName(), to.spelExpression());
		newEntity = entityRepo.save(newEntity);
		return newEntity;		
	}
}

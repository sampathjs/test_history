package com.matthey.pmm.toms.service.conversion;

import java.util.ArrayList;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.AttributeCalculation;
import com.matthey.pmm.toms.repository.AttributeCalculationRepository;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.ImmutableAttributeCalculationTo;

@Service
public class AttributeCalculationConverter extends EntityToConverter<AttributeCalculation, AttributeCalculationTo> {
	@Autowired
	AttributeCalculationRepository entityRepo;
	
	@Override
	public AttributeCalculationTo toTo(AttributeCalculation entity) {
		return ImmutableAttributeCalculationTo.builder()
				.id(entity.getId())
				.addAllDependentAttributes(entity.getDependentAttributes())
				.className(entity.getClassName())
				.spelExpression(entity.getSpelExpression())
				.attributeName(entity.getAttributeName())
				.build();
	}

	@Override
	@Transactional
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
		if (to.id() > 0) {
			newEntity.setId(to.id());			
		}
		newEntity = entityRepo.save(newEntity);
		return newEntity;		
	}
}

package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.transport.ImmutableIndexTo;
import com.matthey.pmm.toms.transport.IndexTo;

@Service
public class IndexConverter extends EntityToConverter<IndexEntity, IndexTo>{
	@Autowired
	private IndexRepository entityRepo;
	
	@Autowired 
	private ReferenceRepository refRepo;

	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}

	@Override
	public IndexTo toTo (IndexEntity entity) {
		return ImmutableIndexTo.builder()
				.idIndexName(entity.getIndexName().getId())
				.idCurrencyTwoName(entity.getCurrencyTwoName().getId())
				.idCurrencyOneName(entity.getCurrencyOneName().getId())
				.id(entity.getId())
				.idLifecycle(entity.getLifecycleStatus().getEndurId())
				.sortColumn(entity.getSortColumn())
				.build();
	}
	
	@Override
	@Transactional
	public IndexEntity toManagedEntity (IndexTo to) {		
		Optional<IndexEntity> existingEntity  = entityRepo.findById(to.id());
		Reference indexName  = loadRef(to, to.idIndexName());
		Reference currencyOneName  = loadRef(to, to.idCurrencyOneName());
		Reference currencyTwoName  = loadRef(to, to.idCurrencyTwoName());
		Reference lifecycleStatus  = loadRef(to, to.idLifecycle());
		
		if (existingEntity.isPresent()) {
			existingEntity.get().setIndexName(indexName);
			existingEntity.get().setCurrencyOneName(currencyOneName);
			existingEntity.get().setCurrencyTwoName(currencyTwoName);
			existingEntity.get().setLifecycleStatus(lifecycleStatus);
			existingEntity.get().setSortColumn(to.sortColumn());
			return existingEntity.get();
		}
		IndexEntity newEntity = new IndexEntity(to.id(), indexName, currencyOneName, currencyTwoName, lifecycleStatus, to.sortColumn());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ImmutableFillTo;

@Service
@Transactional
public class FillConverter extends EntityToConverter<Fill, FillTo>{
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private FillRepository entityRepo;
	
	@Autowired
	private ReferenceRepository refRepo;
	
	
	@Override
	public UserRepository userRepo() {
		return userRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public FillTo toTo (Fill entity) {
		return ImmutableFillTo.builder()
				.lastUpdateDateTime(formatDateTime(entity.getLastUpdateDateTime()))
				.idUpdatedBy(entity.getUpdatedBy().getId())
				.idTrader(entity.getTrader().getId())
				.idTrade(entity.getTradeId())
				.id(entity.getId())
				.fillQuantity(entity.getFillQuantity())
				.fillPrice(entity.getFillPrice())
				.displayStringTrader(entity.getTrader() != null?entity.getTrader().getLastName():null)
				.displayStringUpdatedBy(entity.getUpdatedBy() != null?entity.getUpdatedBy().getLastName():null)
				.idFillStatus(entity.getFillStatus().getId())
				.displayStringFillStatus(entity.getFillStatus().getValue())
				.errorMessage(entity.getErrorMessage())
				.build();
	}
	
	@Override
	@Transactional
	public Fill toManagedEntity (FillTo to) {
		User trader = loadUser(to, to.idTrader());
		User updatedBy = loadUser(to, to.idUpdatedBy());
		Reference fillStatus = loadRef(to,  to.idFillStatus());
		
		Optional<Fill> existingEntity = entityRepo.findById(to.id());
		
		if (existingEntity.isPresent()) {
			existingEntity.get().setFillPrice(to.fillPrice());
			existingEntity.get().setFillQuantity(to.fillQuantity());
			existingEntity.get().setLastUpdateDateTime(super.parseDateTime(to, to.lastUpdateDateTime()));
			existingEntity.get().setTradeId(to.idTrade());
			existingEntity.get().setTrader(trader);
			existingEntity.get().setUpdatedBy(updatedBy);
			existingEntity.get().setFillStatus(fillStatus);
			existingEntity.get().setErrorMessage(to.errorMessage());
			return existingEntity.get();
		}
		Fill newEntity =  new Fill(to.fillQuantity(), to.fillPrice(), to.idTrade(), trader, updatedBy, parseDateTime(to, to.lastUpdateDateTime()),
					fillStatus, to.errorMessage());
		if (to.id() >= 1) {
			newEntity.setId(to.id());
		}
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

package com.matthey.pmm.toms.service.conversion;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;

@Service
public class CreditCheckConverter extends EntityToConverter<CreditCheck, CreditCheckTo>{
	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ReferenceRepository refRepo;

	@Autowired
	private PartyRepository partyRepo;
	
	@Autowired
	private CreditCheckRepository entityRepo;
	
	@Override
	public UserRepository userRepo() {
		return userRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public PartyRepository partyRepo() {
		return partyRepo;
	}
	
	@Override
	public CreditCheckTo toTo (CreditCheck entity) {
		return ImmutableCreditCheckTo.builder()
				.idParty(entity.getParty().getId())
				.runDateTime(super.formatDateTime(entity.getRunDateTime()))
				.idCreditCheckRunStatus(entity.getCreditCheckRunStatus().getId())
				.idCreditCheckOutcome(entity.getCreditCheckOutcome().getId())
				.id(entity.getId())
				.currentUtilization(entity.getCurrentUtilization())
				.creditLimit(entity.getCreditLimit())
				.build();
	}
	
	@Override
	public CreditCheck toManagedEntity (CreditCheckTo to) {	
		Date runDateTime = parseDateTime(to, to.runDateTime());
		Party party = loadParty(to, to.idParty());
		Reference creditCheckRunStatus = loadRef (to, to.idCreditCheckRunStatus());
		Reference creditCheckOutcome = to.idCreditCheckOutcome() != null?loadRef (to, to.idCreditCheckOutcome()):null;
		Optional<CreditCheck> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setCreditCheckOutcome(creditCheckOutcome);
			existingEntity.get().setCreditCheckRunStatus(creditCheckRunStatus);
			existingEntity.get().setCreditLimit(to.creditLimit());
			existingEntity.get().setCurrentUtilization(to.currentUtilization());
			existingEntity.get().setParty(party);
			existingEntity.get().setRunDateTime(runDateTime);
			return existingEntity.get();
		}
		CreditCheck newEntity = new CreditCheck(party, to.creditLimit(), to.currentUtilization(), runDateTime, creditCheckRunStatus, creditCheckOutcome);
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

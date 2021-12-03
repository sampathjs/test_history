package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.ImmutablePartyTo;
import com.matthey.pmm.toms.transport.PartyTo;

@Service
public class PartyConverter extends EntityToConverter<Party, PartyTo>{
	@Autowired
	private PartyRepository partyRepo;
	
	@Autowired 
	private ReferenceRepository refRepo;

	@Autowired 
	private ReferenceTypeRepository refTypeRepo;

	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		return refTypeRepo;
	}
	
	@Override
	public PartyTo toTo (Party entity) {
		return ImmutablePartyTo.builder()
				.id(entity.getId())
				.idLegalEntity(entity.getLegalEntity() != null?entity.getLegalEntity().getId():null)
				.name(entity.getName())
				.typeId(entity.getType().getId())
				.sortColumn(entity.getSortColumn())
				.build();
	}
	
	@Override
	public Party toManagedEntity (PartyTo to) {		
		Optional<Party> existingEntity  = partyRepo.findById(to.id());
		Optional<Party> legalEntity  = partyRepo.findById(to.idLegalEntity());
		Reference type  = loadRef(to, to.typeId());
		Reference lifecycleStatus  = loadRef(to, to.idLifecycle());
		
		Party party;
		if (existingEntity.isPresent()) {
			party = existingEntity.get();
			party.setLegalEntity(legalEntity.isPresent()?legalEntity.get():null);
			party.setType(type);
			party.setName(to.name());
			party.setLifecycleStatus(lifecycleStatus);
			party.setSortColumn(to.sortColumn());
		} else {
			party = new Party(to.id() , to.name(), type, legalEntity.orElse(null), lifecycleStatus, to.sortColumn());
			party = partyRepo.save(party);
		}
		return party;
	}
}

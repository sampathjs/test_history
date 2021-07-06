package com.matthey.pmm.toms.conversion;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.transport.ImmutablePartyTo;
import com.matthey.pmm.toms.transport.PartyTo;

@Service
public class PartyConversion {
	@Autowired
	private PartyRepository partyRepo;
	
	@Autowired 
	private ReferenceRepository refRepo;
	
	public PartyTo toTo (Party entity) {
		return ImmutablePartyTo.builder()
				.id(entity.getId())
				.idLegalEntity(entity.getLegalEntity().getId())
				.name(entity.getName())
				.typeId(entity.getType().getId())
				.build();
	}
	
	public Party toManagedEntity (PartyTo to) {		
		Optional<Party> existingParty  = partyRepo.findById(to.id());
		Optional<Party> legalEntity  = partyRepo.findById(to.idLegalEntity());
		Optional<Reference> type  = refRepo.findById(to.typeId());
		Party party;
		if (existingParty.isPresent()) {
			party = existingParty.get();
			party.setLegalEntity(legalEntity.get());
			party.setType(type.get());
			party.setName(to.name());
		} else {
			party = new Party(to.name(), type.get(), legalEntity.orElse(null));
			party.setId(to.id());
			party = partyRepo.save(party);
		}

		return party;
	}
}

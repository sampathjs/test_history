package com.matthey.pmm.toms.service.conversion;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.ImmutableUserTo;
import com.matthey.pmm.toms.transport.UserTo;

@Service
public class UserConverter extends EntityToConverter<User, UserTo> {
	@Autowired
	private ReferenceRepository refRepo;

	@Autowired
	private ReferenceTypeRepository refTypeRepo;

	@Autowired
	private PartyRepository partyRepo;
	
	@Autowired
	private UserRepository entityRepo;
	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		return refTypeRepo;
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
	public UserTo toTo (User entity) {
		List<Long> tradeableCounterPartyIds = 
				entity.getTradeableParties()
				.stream()
				.filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT.getEntity().id()
						|| x.getType().getId() == DefaultReference.PARTY_TYPE_EXTERNAL_LE.getEntity().id())
				.map(x -> x.getId())
				.collect(Collectors.toList());
		List<Long> tradeableInternalPartyIds = 
				entity.getTradeableParties()
				.stream()
				.filter(x -> x.getType().getId() == DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity().id()
						|| x.getType().getId() == DefaultReference.PARTY_TYPE_INTERNAL_LE.getEntity().id())
				.map(x -> x.getId())
				.collect(Collectors.toList());
		
		return ImmutableUserTo.builder()
				.id(entity.getId())
				.active(entity.getActive())
				.addAllTradeableCounterPartyIds(tradeableCounterPartyIds)
				.addAllTradeableInternalPartyIds(tradeableInternalPartyIds)
				.email(entity.getEmail())
				.firstName(entity.getFirstName())
				.lastName(entity.getLastName())
				.roleId(entity.getRole().getId())
				.build();
	}
	
	@Override
	public User toManagedEntity (UserTo to) {
		Reference role = loadRef(to, to.roleId());
		List<Party> tradeableParties = 
				Streams.concat(to.tradeableCounterPartyIds().stream(), to.tradeableInternalPartyIds().stream())
				.map(x -> super.loadParty(to, x))
				.collect(Collectors.toList());
		
		List<Reference> tradeablePortfolios = 
				to.tradeablePortfolioIds().stream()
				.map(x -> super.loadRef(to, x))
				.collect(Collectors.toList());
		
		Optional<User> entity = entityRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setActive(to.active());
			entity.get().setEmail(to.email());
			entity.get().setFirstName(to.firstName());
			entity.get().setLastName(to.lastName());
			entity.get().setRole(role);
			if (    !entity.get().getTradeableParties().containsAll(tradeableParties) | 
					!tradeableParties.containsAll(entity.get().getTradeableParties())) {
				entity.get().setTradeableParties(tradeableParties);				
			}
			if (    !entity.get().getTradeablePortfolios().containsAll(tradeablePortfolios) | 
					!tradeablePortfolios.containsAll(entity.get().getTradeablePortfolios())) {
				entity.get().setTradeablePortfolios(tradeablePortfolios);				
			}
			return entity.get();
		}
		User newEntity = new User (to.id(), to.email(), to.firstName(), to.lastName(), role, to.active(), tradeableParties, tradeablePortfolios);
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}

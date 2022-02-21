package com.matthey.pmm.toms.service.conversion;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

import org.tinylog.Logger;

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

		List<Long> tradeablePortfolioIds = 
				entity.getTradeablePortfolios()
				.stream()
				.map(x -> x.getId())
				.collect(Collectors.toList());

		
		return ImmutableUserTo.builder()
				.id(entity.getId())
				.idLifecycleStatus(entity.getLifecycleStatus().getId())
				.addAllTradeableCounterPartyIds(tradeableCounterPartyIds)
				.addAllTradeableInternalPartyIds(tradeableInternalPartyIds)
				.addAllTradeablePortfolioIds(tradeablePortfolioIds)
				.email(entity.getEmail())
				.firstName(entity.getFirstName())
				.lastName(entity.getLastName())
				.roleId(entity.getRole().getId())
				.idDefaultInternalBu(entity.getDefaultInternalBu() != null?entity.getDefaultInternalBu().getId():null)
				.idDefaultInternalPortfolio(entity.getDefaultInternalPortfolio() != null?entity.getDefaultInternalPortfolio().getId():null)
				.systemName(entity.getSystemName())
				.build();
	}
	
	@Override
	@Transactional
	public User toManagedEntity (UserTo to) {
		Reference role = loadRef(to, to.roleId());
		Reference lifecycleStatus = loadRef(to, to.idLifecycleStatus());
		Reference defaultInternalPortfolio = to.idDefaultInternalPortfolio() != null?loadRef(to, to.idDefaultInternalPortfolio()):null;
		Party defaultInternalBu = to.idDefaultInternalBu() != null?loadParty(to, to.idDefaultInternalBu()):null;
		Logger.debug("tradeablePortfolio IDs on TO: " + to.tradeablePortfolioIds());
		
		Optional<User> entity = entityRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setLifecycleStatus(lifecycleStatus);
			entity.get().setEmail(to.email());
			entity.get().setFirstName(to.firstName());
			entity.get().setLastName(to.lastName());
			entity.get().setRole(role);
			entity.get().setSystemName(to.systemName());
			Set<Long> tradeablePartyIds= entityRepo.findTradeablePartiesIdById(entity.get().getId());
			Set<Long> tradeablePortfolioIds= entityRepo.findTradeablePortfolioIdById(entity.get().getId());
			Set<Long> allTradeablePartiesOnTo = new HashSet<>(to.tradeableCounterPartyIds());
			allTradeablePartiesOnTo.addAll(to.tradeableInternalPartyIds());
			
			if (    !allTradeablePartiesOnTo.containsAll(tradeablePartyIds) || 
					!tradeablePartyIds.containsAll(allTradeablePartiesOnTo)) {
				entity.get().getTradeableParties().clear();
				Stream.concat(to.tradeableInternalPartyIds().stream(), to.tradeableCounterPartyIds().stream())
						.map(x -> partyRepo.findById(x).get())
						.forEach(x -> {entity.get().getTradeableParties().add(x); });
			}
			if (    !to.tradeablePortfolioIds().containsAll(tradeablePortfolioIds) || 
					!tradeablePortfolioIds.containsAll(to.tradeablePortfolioIds())) {
				entity.get().getTradeablePortfolios().clear();
				to.tradeablePortfolioIds().stream()
						.map(x -> refRepo.findById(x).get())
						.forEach(x -> {entity.get().getTradeablePortfolios().add(x); });
			}
			entity.get().setDefaultInternalBu(defaultInternalBu);
			entity.get().setDefaultInternalPortfolio(defaultInternalPortfolio);
			Logger.debug("Updated User Entity: " + entity.get());
			return entity.get();
		}
		List<Reference> tradeablePortfolios = to.tradeablePortfolioIds().stream()
				.filter(x -> refRepo.existsById(x))
				.map(x -> refRepo.findById(x).get())
				.collect(Collectors.toList());
		List<Party> tradeableParties = Stream.concat(to.tradeableInternalPartyIds().stream(), to.tradeableCounterPartyIds().stream())
				.filter(x -> partyRepo.existsById(x))
				.map(x -> partyRepo.findById(x).get())
				.collect(Collectors.toList());
				
		User newEntity = new User (to.id(), to.email(), to.firstName(), to.lastName(), role, lifecycleStatus, tradeableParties, 
				tradeablePortfolios, defaultInternalBu, defaultInternalPortfolio, to.systemName());
		newEntity = entityRepo.save(newEntity);
		Logger.debug("New User Entity: " + newEntity);
		return newEntity;
	}
}

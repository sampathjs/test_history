package com.matthey.pmm.toms.service.impl;


import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.service.TomsPartyDataService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.transport.PartyTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public abstract class PartyDataControllerImpl implements TomsPartyDataService {
	@Autowired
	protected TomsValidator validator;

	@Autowired 
	protected PartyConverter partyConverter;
	
	@Autowired
	protected PartyRepository partyRepo;
	
	@Override
	@ApiOperation("Retrieval of Parties")
	public Set<PartyTo> getParties (
			@ApiParam(value = "Party Type (Id of an reference of type Party Type), 0 or null = all", example = "2", required = false) @RequestParam(required=false) Long partyTypeId,
			@ApiParam(value = "ID of the legal entity the retrieved parties belong to, 0 or null = all", example = "20039", required = false) @RequestParam(required=false) Long legalEntityId) {
		
		Optional<Reference> partyType = validator.verifyDefaultReference(partyTypeId, 
				Arrays.asList(DefaultReferenceType.PARTY_TYPE),  this.getClass(), "getParties", "partyTypeId", partyTypeId == null || partyTypeId == 0);
		Optional<Party> legalEntity = validator.verifyParty(legalEntityId, 
				Arrays.asList(DefaultReference.PARTY_TYPE_EXTERNAL_LE, DefaultReference.PARTY_TYPE_INTERNAL_LE),  this.getClass(), "getParties", "legalEntityId", legalEntityId == null || legalEntityId == 0);
		
    	if (partyType.isPresent()) {
			if (legalEntity.isPresent()) {
				return partyRepo.findByTypeAndLegalEntity(partyType.get(), legalEntity.get()).stream()
						.map(x -> partyConverter.toTo(x)).collect(Collectors.toSet());
			} else {
				return partyRepo.findByType(partyType.get()).stream()
						.map(x -> partyConverter.toTo(x)).collect(Collectors.toSet());
			}
		} else {
			if (legalEntity.isPresent()) {
				return partyRepo.findByLegalEntity(legalEntity.get()).stream()
						.map(x -> partyConverter.toTo(x)).collect(Collectors.toSet());
			} else {
				return StreamSupport.stream (partyRepo.findAll().spliterator(), false)
						.map(x -> partyConverter.toTo(x)).collect(Collectors.toSet());
			}
		}
	}
}
package com.matthey.pmm.toms.service.common;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;

@Service
public class Validator {
	@Autowired
	ReferenceRepository refRepo;

	@Autowired
	ReferenceTypeRepository refTypeRepo;
	
	@Autowired
	PartyRepository partyRepo;
	
	/**
	 * Verifies a provided reference is present in the database and has the type of one of the provided
	 * expectedRefTypes.
	 * @param refId ID of the reference to check.
	 * @param expectedRefTypes List of expected reference types.
	 * @param clazz The class object of the calling class
	 * @param method The method of the calling class.
	 * @param parameter The name of the parameter of the method of 
	 * the calling class that is being checked.
	 * @return always true or it throws either an IllegalReferenceException or an 
	 * IllegalReferenceTypeException
	 */
	public Optional<Reference> verifyDefaultReference (Long refId, List<DefaultReferenceType> expectedRefTypes,
			Class clazz, String method, String parameter, boolean isOptional) {
		if (refId != null && refId !=  0) {
			
			Optional<Reference> reference = refRepo.findById(refId);
			if (!reference.isPresent()) {
				if (!isOptional) {
					throw new IllegalReferenceException(clazz, method, parameter, 
							"(Several)", "Unknown(" + refId + ")");					
				} else {
					return reference;
				}
			}
			Reference ref = reference.get();
			List<Long> expectedRefTypeIds = expectedRefTypes.stream()
					.map(x -> x.getEntity().id())
					.collect(Collectors.toList());
			String expectedRefTypesString =	expectedRefTypes.stream()
				.map(x -> "" + x.getEntity().name() + "(" + x.getEntity().id() + ")")
				.collect(Collectors.joining("/"))
				;
			
			if (!expectedRefTypeIds.contains(ref.getType().getId())) {
				Optional<ReferenceType> refType = refTypeRepo.findById (ref.getType().getId());
				String refTypeName = "Unknown";
				String refTypeId = "Unknown";
				if (refType.isPresent()) {
					refTypeName = refType.get().getName();
					refTypeId = "" + refType.get().getId();
				}
				throw new IllegalReferenceTypeException(clazz, method, parameter,
						expectedRefTypesString, 
						refTypeName + "(" + refTypeId + ") of reference " + ref.getValue() + "(" + ref.getId() + ")" );
			} else {
				return reference;
			}
		}
		return  Optional.empty();
	}

	/**
	 * Ensures the provided party exists and belongs to the provided allowedPartyTypes
	 * @param partyId
	 * @param allowedPartyTypes
	 * @param clazz
	 * @param method
	 * @param parameter
	 * @param isOptional
	 * @return
	 */
	public Optional<Party> verifyParty(Long partyId, List<DefaultReference> allowedPartyTypes,
			Class clazz, String method, String parameter, boolean isOptional) {
		if (partyId != null && partyId !=  0) {
			Optional<Party> partyOpt = partyRepo.findById(partyId);
			if (!partyOpt.isPresent()) {
				if (!isOptional) {
					throw new IllegalIdException(clazz, method, parameter, 
							"(Several)", "Unknown(" + partyId + ")");					
				} else {
					return partyOpt;
				}
			}
			Party party = partyOpt.get();
			List<Long> expectedRefTypeIds = allowedPartyTypes.stream()
					.map(x -> x.getEntity().id())
					.collect(Collectors.toList());
			String expectedRefTypesString =	allowedPartyTypes.stream()
				.map(x -> "" + x.getEntity().name() + "(" + x.getEntity().id() + ")")
				.collect(Collectors.joining("/"))
				;
			
			if (!expectedRefTypeIds.contains(party.getType().getId())) {
				Optional<Reference> ref = refRepo.findById (party.getType().getId());
				String refName = "Unknown";
				String refId = "Unknown";
				if (ref.isPresent()) {
					refName = ref.get().getValue();
					refId = "" + ref.get().getId();
				}
				throw new IllegalReferenceException(clazz, method, parameter,
						expectedRefTypesString, 
						refName + "(" + refId + ") of reference " + party.getName() + "(" + party.getId() + ")" );
			} else {
				return partyOpt;
			}
		}
		return  Optional.empty();
	}

}

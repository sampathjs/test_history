package com.matthey.pmm.toms.service.mock;


import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.enums.DefaultParty;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.enums.DefaultReferenceType;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.enums.DefaultReference;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.matthey.pmm.toms.service.TomsPartyDataService;
import com.matthey.pmm.toms.service.TomsStaticDataService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
public class MockPartyDataController implements TomsPartyDataService {
	@Override
	@ApiOperation("Retrieval of Parties")
	public Set<Party> getParties (
			@ApiParam(value = "Party Type, 0 or null = all", example = "2", required = false) @RequestParam(required=false) Integer partyTypeId) {
		if (partyTypeId != null && partyTypeId !=  0) {
			Optional<Reference> reference = DefaultReference.findById(partyTypeId);
			if (!reference.isPresent()) {
				throw new IllegalReferenceException(this.getClass(), "getParties", "partyTypeId", 
						"(Several)", "Unknown(" + partyTypeId + ")");
			}
			Reference ref = reference.get();
			if (ref.typeId() != DefaultReferenceType.PartyType.getEntity().id()) {
				Optional<ReferenceType> refType = DefaultReferenceType.findById (ref.typeId());
				String refTypeName = "Unknown";
				if (refType.isPresent()) {
					refTypeName = refType.get().name();
				}
				throw new IllegalReferenceTypeException(this.getClass(), "getParties", "partyTypeId", 
						DefaultReferenceType.PartyType.getEntity().name(), refTypeName + "(" + partyTypeId + ")");
			} else {
				return new HashSet<>(DefaultParty.asList().stream().filter(x -> x.typeId() == partyTypeId).collect(Collectors.toList()));				
			}
		} else {
			return new HashSet<>(DefaultParty.asList());
		}
	}
}

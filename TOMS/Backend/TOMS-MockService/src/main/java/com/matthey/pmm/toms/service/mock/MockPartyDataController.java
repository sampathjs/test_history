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

import com.matthey.pmm.toms.service.TomsService;

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
			@ApiParam(value = "Party Type (Id of an reference of type Party Type), 0 or null = all", example = "2", required = false) @RequestParam(required=false) Integer partyTypeId,
			@ApiParam(value = "ID of the legal entity the retrieved parties belong to, 0 or null = all", example = "20039", required = false) @RequestParam(required=false) Integer legalEntityId) {
		if (TomsService.verifyDefaultReference (partyTypeId, 
				Arrays.asList(DefaultReferenceType.PARTY_TYPE),
				this.getClass(), "getParties","partyTypeId")) {			
			if (legalEntityId != null && legalEntityId != 0) {
				return new HashSet<>(DefaultParty.asList().stream().filter(x -> x.typeId() == partyTypeId && x.legalEntity() == legalEntityId).collect(Collectors.toList()));
			} else {
				return new HashSet<>(DefaultParty.asList().stream().filter(x -> x.typeId() == partyTypeId).collect(Collectors.toList()));					
			}			
		} else {
			if (legalEntityId != null && legalEntityId != 0) {
				return new HashSet<>(DefaultParty.asList().stream().filter(x -> x.legalEntity() == legalEntityId).collect(Collectors.toList()));
			} else {
				return new HashSet<>(DefaultParty.asList());				
			}
		}
	}
}
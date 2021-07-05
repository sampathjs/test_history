package com.matthey.pmm.toms.service.mock;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsPartyDataService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.transport.PartyTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockPartyDataController implements TomsPartyDataService {
	@Override
	@ApiOperation("Retrieval of Parties")
	public Set<PartyTo> getParties (
			@ApiParam(value = "Party Type (Id of an reference of type Party Type), 0 or null = all", example = "2", required = false) @RequestParam(required=false) Long partyTypeId,
			@ApiParam(value = "ID of the legal entity the retrieved parties belong to, 0 or null = all", example = "20039", required = false) @RequestParam(required=false) Long legalEntityId) {
		if (TomsService.verifyDefaultReference (partyTypeId, 
				Arrays.asList(DefaultReferenceType.PARTY_TYPE),
				this.getClass(), "getParties","partyTypeId", false)) {			
			if (legalEntityId != null && legalEntityId != 0) {
				return new HashSet<>(TestParty.asList().stream().filter(x -> x.typeId() == partyTypeId && x.idLegalEntity() == legalEntityId).collect(Collectors.toList()));
			} else {
				return new HashSet<>(TestParty.asList().stream().filter(x -> x.typeId() == partyTypeId).collect(Collectors.toList()));					
			}			
		} else {
			if (legalEntityId != null && legalEntityId != 0) {
				return new HashSet<>(TestParty.asList().stream().filter(x -> x.idLegalEntity() == legalEntityId).collect(Collectors.toList()));
			} else {
				return new HashSet<>(TestParty.asList());				
			}
		}
	}
}
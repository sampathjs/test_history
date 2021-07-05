package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.PartyTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Party Data"}, description = "APIs for relevant party data")
@RequestMapping(API_PREFIX)
public interface TomsPartyDataService {
    @Cacheable({"Parties"})
    @ApiOperation("Retrieval of Parties")
	@GetMapping("/parties")
	public Set<PartyTo> getParties (
			@ApiParam(value = "Party Type (Id of an reference of type Party Type), 0 or null = all", example = "2", required = false) @RequestParam(required=false) Long partyTypeId,
			@ApiParam(value = "ID of the legal entity the retrieved parties belong to, 0 or null = all", example = "20039", required = false) @RequestParam(required=false) Long legalEntityId);
}

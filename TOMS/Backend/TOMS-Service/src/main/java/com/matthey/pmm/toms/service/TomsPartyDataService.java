package com.matthey.pmm.toms.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

@Api(tags = {"Party Data"}, description = "APIs for relevant party data")
@RequestMapping(API_PREFIX)
public interface TomsPartyDataService {
    @Cacheable({"Parties"})
    @ApiOperation("Retrieval of Parties")
	@GetMapping("/parties")
	public Set<PartyTo> getParties (
			@ApiParam(value = "Party Type (Id of an reference of type Party Type), 0 or null = all", example = "2", required = false) @RequestParam(required=false) Integer partyTypeId,
			@ApiParam(value = "ID of the legal entity the retrieved parties belong to, 0 or null = all", example = "20039", required = false) @RequestParam(required=false) Integer legalEntityId);
}

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
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.cache.annotation.Cacheable;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.Party;

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
	public Set<Party> getParties (
			@ApiParam(value = "Party Type, 0 or null = all", example = "2", required = false) @RequestParam(required=false) Integer partyTypeId,
			@ApiParam(value = "Legal Entity ID, 0 or null = all", example = "20039", required = false) @RequestParam(required=false) Integer legalEntityId);
}

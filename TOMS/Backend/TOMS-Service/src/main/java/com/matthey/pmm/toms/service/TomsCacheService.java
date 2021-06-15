package com.matthey.pmm.toms.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.cache.annotation.Cacheable;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.Party;

import java.util.Map;
import java.util.Set;
import java.util.Date;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

@Api(tags = {"Cache Controller (Global)"}, description = "APIs to clear cache for all users and receive feedback which local session cache categories had been invalidated")
@RequestMapping(API_PREFIX)
public interface TomsCacheService {
    @ApiOperation("Retrieval of cache categories that have been invalidated")
	@GetMapping("/cacheInvalidate")
	public Set<Integer> getInvalidatedCacheCategories (
			@ApiParam(value = "Cut off date, all cache categories returned have been invalidated after that date. Format 'yyyy-MM-dd'T'hh:mm:ss'", example = "2000-10-31T01:30:00", required = true)
			@RequestParam("cutOffDate") 
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String cutOffDate);
    
    @ApiOperation("Invalidated cache categories for all users / services")
    @PatchMapping("/cacheInvalidate")
	public void patchInvalidatedCacheCategories (@ApiParam(value = "Set of IDs of References of Reference Type 'Cache Category'", example = "[21, 22, 23]", required = true) @RequestBody(required=true) Set<Integer> invalidatedCacheCategories);
}

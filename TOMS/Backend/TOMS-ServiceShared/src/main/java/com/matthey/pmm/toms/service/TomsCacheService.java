package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Cache Controller (Global)"}, description = "APIs to clear cache for all users and receive feedback which local session cache categories had been invalidated")
@RequestMapping(API_PREFIX)
public interface TomsCacheService {
    @ApiOperation("Retrieval of cache categories that have been invalidated")
	@GetMapping("/cacheInvalidate")
	public Set<Long> getInvalidatedCacheCategories (
			@ApiParam(value = "Cut off date, all cache categories returned have been invalidated after that date. Format 'yyyy-MM-dd'T'hh:mm:ss'", example = "2000-10-31T01:30:00", required = false)
			@RequestParam(name="cutOffDate", required=false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String cutOffDate);
    
    @ApiOperation("Invalidated cache categories for all users / services")
    @PatchMapping("/cacheInvalidate")
	public void patchInvalidatedCacheCategories (@ApiParam(value = "Set of IDs of References of Reference Type 'Cache Category'", example = "[21, 22, 23]", required = true) @RequestBody(required=true) Set<Long> invalidatedCacheCategories);
}

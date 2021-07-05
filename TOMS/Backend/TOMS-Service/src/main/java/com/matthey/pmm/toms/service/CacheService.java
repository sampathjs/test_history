package com.matthey.pmm.toms.service;

import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class CacheService implements TomsCacheService {   	
	@ApiOperation("Retrieval of cache categories that have been invalidated")
	public Set<Long> getInvalidatedCacheCategories (
			@ApiParam(value = "Cut off date, all cache categories returned have been invalidated after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = true)
			@RequestParam("cutOffDate")
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String cutOffDate) {
		return null;
    }
    
    @ApiOperation("Invalidated cache categories for all users / services")
	public void patchInvalidatedCacheCategories (@ApiParam(value = "Set of IDs of References of Reference Type 'Cache Category'", example = "[21, 22, 23]", required = true) @RequestBody(required=true) Set<Long> invalidatedCacheCategories) {
    	return;
    }
}

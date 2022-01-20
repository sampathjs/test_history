package com.matthey.pmm.toms.service.impl;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.CacheInvalidate;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.CacheInvalidateRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.TomsCacheService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public abstract class CacheServiceImpl implements TomsCacheService {    
    @Autowired
    protected CacheInvalidateRepository cacheInvalidateRepo;
    
    @Autowired
    protected ReferenceRepository refRepo;
    
    @Autowired
    protected TomsValidator validator;    
    
	@ApiOperation("Retrieval of cache categories that have been invalidated. Return value contains IDs of references of type CacheCategory")
	public Set<Long> getInvalidatedCacheCategories (
			@ApiParam(value = "Cut off date, all cache categories returned have been invalidated after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = false)
			@RequestParam(name="cutOffDate", required=false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String cutOffDate) {	
		if (cutOffDate != null && cutOffDate.trim().length() > 0) {
			SimpleDateFormat sdf = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
			try {
				Date parsedTime = sdf.parse (cutOffDate);
				return cacheInvalidateRepo.findByCutOffDateTimeAfter(parsedTime).stream()
					.map(x -> x.getCacheCategory().getId())
					.collect(Collectors.toSet());
			} catch (ParseException pe) {
				throw new IllegalDateFormatException (this.getClass(), "getInvalidatedCacheCategories", "cutOffDate", TomsService.DATE_TIME_FORMAT, cutOffDate);
			}			
		}
		return cacheInvalidateRepo.findDistinctCacheCategoryId();
    }
    
    @ApiOperation("Invalidated cache categories for all users / services")
	public void patchInvalidatedCacheCategories (@ApiParam(value = "Set of IDs of References of Reference Type 'Cache Category'", example = "[21, 22, 23]", required = true) @RequestBody(required=true) Set<Long> invalidatedCacheCategories) {
    	Set<Reference> cacheCategories = new HashSet<>();
    	for (Long referenceId : invalidatedCacheCategories) {
			Optional<Reference> reference = 
					validator.verifyDefaultReference(referenceId, Arrays.asList(DefaultReferenceType.CACHE_TYPE), getClass(), "patchInvalidatedCacheCategories", "invalidatedCacheCategories", false);
			cacheCategories.add(reference.get());
    	}
    	Date insertionDate = new Date();
    	List<CacheInvalidate> cacheInvalidates = cacheCategories.stream()
    			.map(x -> new CacheInvalidate(x, insertionDate))
    			.collect(Collectors.toList());
    	cacheInvalidateRepo.saveAll(cacheInvalidates);
    }
}

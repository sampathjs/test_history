package com.matthey.pmm.toms.service.mock;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsCacheService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockCacheService implements TomsCacheService {
    private static final Map<Integer, Date> INVALIDATED_CACHE_CATEGORIES = new ConcurrentHashMap<>(); 
    	
	@ApiOperation("Retrieval of cache categories that have been invalidated")
	public Set<Integer> getInvalidatedCacheCategories (
			@ApiParam(value = "Cut off date, all cache categories returned have been invalidated after that date. Format 'yyyy-MM-dd hh:mm:ss' (UTC)", example = "2000-10-31 01:30:00", required = true)
			@RequestParam("cutOffDate")
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) String cutOffDate) {
		SimpleDateFormat sdf = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
		try {
			Date parsedTime = sdf.parse (cutOffDate);
			Set<Integer> invalidatedCacheCategories = INVALIDATED_CACHE_CATEGORIES.entrySet().stream()
					.filter(x -> x.getValue().compareTo(parsedTime) >= 0)
					.map(x -> x.getKey())
					.collect(Collectors.toSet());
	    	return invalidatedCacheCategories; 			
		} catch (ParseException pe) {
			throw new IllegalDateFormatException (this.getClass(), "getInvalidatedCacheCategories", "cutOffDate", TomsService.DATE_TIME_FORMAT, cutOffDate);
		}
    }
    
    @ApiOperation("Invalidated cache categories for all users / services")
	public void patchInvalidatedCacheCategories (@ApiParam(value = "Set of IDs of References of Reference Type 'Cache Category'", example = "[21, 22, 23]", required = true) @RequestBody(required=true) Set<Integer> invalidatedCacheCategories) {
    	List<Integer> tempList = new ArrayList<>(invalidatedCacheCategories.size());
    	for (Integer referenceId : invalidatedCacheCategories) {
			Optional<ReferenceTo> reference = DefaultReference.findById(referenceId);
			if (!reference.isPresent()) {
				throw new IllegalReferenceException(this.getClass(), "patchInvalidatedCacheCategories", "invalidatedCacheCategories", 
						"(Several)", "Unknown(" + referenceId + ")");
			}
			ReferenceTo ref = reference.get();
			if (ref.idType() != DefaultReferenceType.CACHE_TYPE.getEntity().id()) {
				Optional<ReferenceTypeTo> refType = DefaultReferenceType.findById (ref.idType());
				String refTypeName = "Unknown";
				if (refType.isPresent()) {
					refTypeName = refType.get().name();
				}
				throw new IllegalReferenceTypeException(this.getClass(), "patchInvalidatedCacheCategories", "invalidatedCacheCategories", 
						DefaultReferenceType.CACHE_TYPE.getEntity().name(), refTypeName + "(" + referenceId + ")");
			} else {
				tempList.add (referenceId);
			}
    	}
    	Date insertionDate = new Date();
    	tempList.forEach (x ->  INVALIDATED_CACHE_CATEGORIES.put(x, insertionDate));
    }
}

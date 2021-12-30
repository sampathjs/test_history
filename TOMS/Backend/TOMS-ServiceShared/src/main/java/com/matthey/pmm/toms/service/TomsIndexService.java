package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.IndexTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Index Data"}, description = "APIs for relevant index data")
@RequestMapping(API_PREFIX)
public interface TomsIndexService {
    @Cacheable({"Index"})
    @ApiOperation("Retrieval of Index Data")
	@GetMapping("/index")
	public Set<IndexTo> getIndexes (
			@ApiParam(value = "One of the currencies IDs the indexes must have. Null or 0 = all indexes", example = "42", required = false) @RequestParam(required=false) Long currencyRefId);
}

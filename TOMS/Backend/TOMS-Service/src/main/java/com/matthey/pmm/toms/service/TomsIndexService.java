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
import com.matthey.pmm.toms.model.Index;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

@Api(tags = {"Index Data"}, description = "APIs for relevant index data")
@RequestMapping(API_PREFIX)
public interface TomsIndexService {
    @Cacheable({"Index"})
    @ApiOperation("Retrieval of Index Data")
	@GetMapping("/index")
	public Set<Index> getIndexes (
			@ApiParam(value = "One of the currencies of the indexes must have the provided ID. Null or 0 = all indexes", example = "42", required = false) @RequestParam(required=false) Integer currencyRefId);
}

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

import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

@Api(tags = {"Metadata"}, description = "APIs for providing process related metadata")
@RequestMapping(API_PREFIX)
public interface TomsMetadataService {
    @Cacheable({"ProcessTransiton"})
    @ApiOperation("Process Status Transition Data Retrieval")
	@GetMapping("/processTransition")
	public Set<ProcessTransitionTo> getProcessTransitions (
			@ApiParam(value = "Transition Category Id, 0 or null = all ", example = "111", required = false) @RequestParam(required=false) Integer referenceCategoryId);
}

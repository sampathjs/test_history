package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.ProcessTransitionTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Metadata"}, description = "APIs for providing process related metadata")
@RequestMapping(API_PREFIX)
public interface TomsMetadataService {
    @Cacheable({"ProcessTransiton"})
    @ApiOperation("Process Status Transition Data Retrieval")
	@GetMapping("/processTransition")
	public Set<ProcessTransitionTo> getProcessTransitions (
			@ApiParam(value = "Transition Category Id, 0 or null = all ", example = "111", required = false) @RequestParam(required=false) Integer referenceCategoryId);
}

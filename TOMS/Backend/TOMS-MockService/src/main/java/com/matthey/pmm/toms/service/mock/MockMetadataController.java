package com.matthey.pmm.toms.service.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultAttributeCalculation;
import com.matthey.pmm.toms.enums.v1.DefaultProcessTransition;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsMetadataService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockMetadataController implements TomsMetadataService {
	@ApiOperation("Process Status Transition Data Retrieval")
	public Set<ProcessTransitionTo> getProcessTransitions(
			@ApiParam(value = "Transition Category Id, 0 or null = all ", example = "111", required = false) @RequestParam(required = false) Long referenceCategoryId) {

		if (TomsService.verifyDefaultReference(referenceCategoryId,
				Arrays.asList(DefaultReferenceType.PROCESS_TRANSITION_TYPE), this.getClass(), "getProcessTransitions",
				"referenceCategoryId", false)) {
			return new HashSet<>(DefaultProcessTransition.asList().stream()
					.filter(x -> x.referenceCategoryId() == referenceCategoryId).collect(Collectors.toList()));
		} else {
			return new HashSet<>(DefaultProcessTransition.asList());
		}
	}
	
    @ApiOperation("Retrieval of the calculation logic for attributes of different entites, especially orders")
	public Set<AttributeCalculationTo> getAttributeCalculations (
			@ApiParam(value = "Optional class name to restrict results for a single class", example = "com.matthey.pmm.toms.transport.ImmutableLimitOrderTo", required = false) @RequestParam(required=false) String className) {
    	if (className != null) {
        	return new HashSet<>(DefaultAttributeCalculation.asListByClassName(className));    		
    	} else {
        	return new HashSet<>(DefaultAttributeCalculation.asList());    		
    	}
    }
}

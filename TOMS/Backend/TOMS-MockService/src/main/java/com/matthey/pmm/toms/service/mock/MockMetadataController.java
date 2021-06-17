package com.matthey.pmm.toms.service.mock;

import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.enums.DefaultReference;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.matthey.pmm.toms.service.TomsMetadataService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTypeTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;

import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.enums.DefaultProcessTransition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
public class MockMetadataController implements TomsMetadataService {
	@ApiOperation("Process Status Transition Data Retrieval")
	public Set<ProcessTransitionTo> getProcessTransitions(
			@ApiParam(value = "Transition Category Id, 0 or null = all ", example = "111", required = false) @RequestParam(required = false) Integer referenceCategoryId) {

		if (TomsService.verifyDefaultReference(referenceCategoryId,
				Arrays.asList(DefaultReferenceType.PROCESS_TRANSITION_TYPE), this.getClass(), "getProcessTransitions",
				"referenceCategoryId")) {
			return new HashSet<>(DefaultProcessTransition.asList().stream()
					.filter(x -> x.referenceCategoryId() == referenceCategoryId).collect(Collectors.toList()));
		} else {
			return new HashSet<>(DefaultProcessTransition.asList());
		}
	}
}

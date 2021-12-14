package com.matthey.pmm.toms.service.impl;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.AttributeCalculationRepository;
import com.matthey.pmm.toms.repository.ProcessTransitionRepository;
import com.matthey.pmm.toms.service.TomsMetadataService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.AttributeCalculationConverter;
import com.matthey.pmm.toms.service.conversion.ProcessTransitionConverter;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public abstract class MetadataControllerImpl implements TomsMetadataService {
	@Autowired
	protected Validator validator;
	
	@Autowired
	protected ProcessTransitionRepository processTransitionRepo;
	
	@Autowired
	protected AttributeCalculationRepository attributeCalculationRepo;
	
	@Autowired
	protected ProcessTransitionConverter processTransitionConverter;

	@Autowired
	protected AttributeCalculationConverter attributeCalculationConverter;
	
	@ApiOperation("Process Status Transition Data Retrieval")
	public Set<ProcessTransitionTo> getProcessTransitions(
			@ApiParam(value = "Transition Category Id, 0 or null = all ", example = "111", required = false) @RequestParam(required = false) Long referenceCategoryId) {
		Optional<Reference> refCategory = validator.verifyDefaultReference(referenceCategoryId,
				Arrays.asList(DefaultReferenceType.PROCESS_TRANSITION_TYPE), this.getClass(), "getProcessTransitions",
				"referenceCategoryId", false);
		if (refCategory.isPresent()) {
			return processTransitionRepo.findByReferenceCategory(refCategory.get()).stream()
					.map(x -> processTransitionConverter.toTo(x))
					.collect(Collectors.toSet());
		} else {
			return StreamSupport.stream(processTransitionRepo.findAll().spliterator(), false)
					.map(x -> processTransitionConverter.toTo(x))
					.collect(Collectors.toSet());
		}
	}
	
    @ApiOperation("Retrieval of the calculation logic for attributes of different entites, especially orders")
	public Set<AttributeCalculationTo> getAttributeCalculations (
			@ApiParam(value = "Optional class name to restrict results for a single class", example = "com.matthey.pmm.toms.transport.ImmutableLimitOrderTo", required = false) @RequestParam(required=false) String className) {
    	if (className != null) {
			return attributeCalculationRepo.findByClassName(className).stream()
					.map(x -> attributeCalculationConverter.toTo(x))
					.collect(Collectors.toSet());
    	} else {
			return StreamSupport.stream(attributeCalculationRepo.findAll().spliterator(), false)
					.map(x -> attributeCalculationConverter.toTo(x))
					.collect(Collectors.toSet());
    	}
    }    
}

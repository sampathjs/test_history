package com.matthey.pmm.toms.service.mock;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.enums.DefaultReferenceType;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.enums.DefaultReference;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.matthey.pmm.toms.service.TomsStaticDataService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class MockStaticDataController implements TomsStaticDataService {

	@Override
	public Set<ReferenceType> getAllReferenceTypes () {
		return new HashSet<>(DefaultReferenceType.asList());
	}
	
	@Override
	@ApiOperation("Retrieval of References")
	public Set<Reference> getReferences (
			@ApiParam(value = "Reference Type ID, 0 or null = all", example = "1", required = false) @RequestParam(required=false) Integer referenceTypeId) {
		if (referenceTypeId != null && referenceTypeId !=  0) {
			return new HashSet<>(DefaultReference.asList().stream().filter(x -> x.typeId() == referenceTypeId).collect(Collectors.toList()));			
		} else {
			return new HashSet<>(DefaultReference.asList());
		}
	}
}

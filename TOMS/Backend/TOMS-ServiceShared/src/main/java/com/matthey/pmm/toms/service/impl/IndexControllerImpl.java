package com.matthey.pmm.toms.service.impl;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.service.TomsIndexService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.IndexConverter;
import com.matthey.pmm.toms.transport.IndexTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@RestController
public abstract class IndexControllerImpl implements TomsIndexService {
	@Autowired
	protected Validator validator;

	@Autowired
	protected IndexRepository indexRepo;
	
	@Autowired
	protected IndexConverter indexConverter;
	
    @ApiOperation("Retrieval of Index Data")
	public Set<IndexTo> getIndexes (
			@ApiParam(value = "One of the currencies of the indexes must have the provided ID. Null or 0 = all indexes", example = "42", required = false) @RequestParam(required=false) Long currencyRefId) {
		Optional<Reference> currencyRef =  
				validator.verifyDefaultReference(currencyRefId, Arrays.asList(DefaultReferenceType.CCY_METAL, DefaultReferenceType.CCY_CURRENCY), this.getClass(), "getIndexes", "currencyRefId", true);
    	if (currencyRef.isPresent()) {
    		return indexRepo.findByCurrencyOneNameOrCurrencyTwoName(currencyRef.get(), currencyRef.get()).stream()
    				.map(x -> indexConverter.toTo(x))
    				.collect(Collectors.toSet());
		} else {
			return StreamSupport.stream(indexRepo.findAll().spliterator(), false)
					.map(x -> indexConverter.toTo(x))
					.collect(Collectors.toSet());
		}
	}
}

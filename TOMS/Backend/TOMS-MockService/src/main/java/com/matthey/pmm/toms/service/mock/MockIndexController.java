package com.matthey.pmm.toms.service.mock;


import com.matthey.pmm.toms.model.Index;
import com.matthey.pmm.toms.enums.DefaultIndex;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.enums.DefaultReferenceType;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.enums.DefaultReference;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.matthey.pmm.toms.service.TomsIndexService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;

import com.matthey.pmm.toms.service.TomsService;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;


@RestController
public class MockIndexController implements TomsIndexService {
    @ApiOperation("Retrieval of Index Data")
	public Set<Index> getIndexes (
			@ApiParam(value = "One of the currencies of the indexes must have the provided ID. Null or 0 = all indexes", example = "42", required = false) @RequestParam(required=false) Integer currencyRefId) {
		if (TomsService.verifyDefaultReference (currencyRefId,
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL),
				this.getClass(), "getIndexes", "currencyRefId")) {
			return new HashSet<>(DefaultIndex.asList().stream().filter(x -> x.idCurrencyOneName() == currencyRefId || x.idCurrencyTwoName() == currencyRefId).collect(Collectors.toList()));
		} else {
			return new HashSet<>(DefaultIndex.asList());
		}
	}
}

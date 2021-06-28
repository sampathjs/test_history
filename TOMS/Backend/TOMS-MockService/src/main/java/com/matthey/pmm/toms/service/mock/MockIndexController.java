package com.matthey.pmm.toms.service.mock;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsIndexService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.mock.testdata.TestIndex;
import com.matthey.pmm.toms.transport.IndexTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@RestController
public class MockIndexController implements TomsIndexService {
    @ApiOperation("Retrieval of Index Data")
	public Set<IndexTo> getIndexes (
			@ApiParam(value = "One of the currencies of the indexes must have the provided ID. Null or 0 = all indexes", example = "42", required = false) @RequestParam(required=false) Integer currencyRefId) {
		if (TomsService.verifyDefaultReference (currencyRefId,
				Arrays.asList(DefaultReferenceType.CCY_CURRENCY, DefaultReferenceType.CCY_METAL),
				this.getClass(), "getIndexes", "currencyRefId", false)) {
			return new HashSet<>(TestIndex.asList().stream().filter(x -> x.idCurrencyOneName() == currencyRefId || x.idCurrencyTwoName() == currencyRefId).collect(Collectors.toList()));
		} else {
			return new HashSet<>(TestIndex.asList());
		}
	}
}

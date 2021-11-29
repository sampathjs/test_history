package com.matthey.pmm.lims;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.matthey.pmm.lims.service.LimsService.API_PREFIX;

@SuppressWarnings("deprecation")
@Api(tags = {"Events"}, description = "APIs for relevant data of events")
@RestController
@RequestMapping(API_PREFIX)
public class LimsController extends AbstractLimsController {

    public LimsController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }

	@Cacheable({ "Results" })
	@ApiOperation("Update Lims Result")
	@PostMapping("/Results")
	public String getLimsResults(
			@ApiParam(value = "Lims Result", example = "975718, CALSPEC2, Ag, PPMMETAL,ND, A", required = true) @RequestParam String result) {
		var limsResult = endurConnector.get("/Results?result={result}", LimsResult[].class, result);
		return genResponse(limsResult, LimsResult.class);
	}

	@Cacheable({ "Sample1" })
	@ApiOperation("Update Lims Sample")
	@PostMapping("Sample1")
	public String postLimsSample(
			@ApiParam(value = "JM Batch Id", example = "ZF0247B", required = true) @RequestParam String batchId,
			@ApiParam(value = "Sample Number", example = "975718", required = true) @RequestParam String sampleNumber,
			@ApiParam(value = "Product", example = "173500", required = true) @RequestParam String product) {
		System.out.print("Testing ... 1 ");
		var limsSample = endurConnector.post(
				"/Sample1?batchId={batchId}&&sampleNumber={sampleNumber}&&product={product}"
				, LimsSample[].class
				, batchId
				, sampleNumber
				, product
				);
		System.out.print("Testing ... 2 ");
		return genResponse(limsSample, LimsSample.class);
	}
	
	
	@Cacheable({ "SampleResults" })
	@ApiOperation("Update Lims SampleResult")
	@PostMapping("/SampleResults")
	public String postLimsSampleResult(
			@ApiParam(value = "JM Batch Id", example = "ZF0247B", required = true) @RequestBody LimsSample sample
			) {
		var limsSampleResult = endurConnector.post(
				"/SampleResults"
				, LimsSample[].class
				, sample
				);
		return genResponse(limsSampleResult, LimsSample.class);
	}
	
	@Cacheable({ "SampleResults" })
	@ApiOperation("Update Lims SampleResult")
	@GetMapping("/SampleResultsTest")
	public LimsSample getLimsSampleResult() {

		return ImmutableLimsSample.builder()
				.product("SampleProduct")
				.sampleNumber("SampleNumber")
				.jmBatchId("BatchId")
				.addResult(ImmutableLimsResult.builder()
							.analysis("Analysis")
							.imputityName("immpurity")
							.sampl00001("Sample")
							.status("status")
							.units("uunit")
							.build())
				.build();
	}

}

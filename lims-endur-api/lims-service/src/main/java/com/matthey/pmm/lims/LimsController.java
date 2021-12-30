package com.matthey.pmm.lims;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

	private static final Logger logger = LogManager.getLogger(LimsController.class);
	
    public LimsController(EndurConnector endurConnector, XmlMapper xmlMapper) {
        super(endurConnector, xmlMapper);
    }
    
/*
	@Cacheable({ "/Sample" })
	@ApiOperation("Update Lims Sample")
	@PostMapping("Sample")
	public String postLimsSample(
			@ApiParam(value = "JM Batch Id", example = "ZF0247B", required = true) @RequestParam String batchId,
			@ApiParam(value = "Sample Number", example = "975718", required = true) @RequestParam String sampleNumber,
			@ApiParam(value = "Product", example = "173500", required = true) @RequestParam String product) {
		logger.info("Post Method postLimsSample() started");
		var limsSample = endurConnector.post(
				"/Sample?batchId={batchId}&&sampleNumber={sampleNumber}&&product={product}"
				, String.class
				, batchId
				, sampleNumber
				, product
				);
		logger.info("Post method postLimsSample completed return status :" + limsSample);
		return limsSample;
	}
	
	@Cacheable({ "/Results" })
	@ApiOperation("Update Lims Result")
	@PostMapping("/Results")
	public String postLimsResults(
			@ApiParam(value = "Lims Result", example = "975718123, CALSPEC2, Ag, PPMMETAL,ND, A1", required = true) @RequestParam String result) {
		logger.info("Post Method postLimsResults() started");
		var limsResult = endurConnector.post("/Results?result={result}", String.class, result);
		logger.info("Post method postLimsResults completed return status :" + limsResult);
		return limsResult;
	}
*/	

	@Cacheable({ "/SampleResults" })
	@ApiOperation("Update Lims SampleResult")
	@PostMapping("/SampleResults")
	public String postLimsSampleResult1(
		@ApiParam(value = "sample Result XML", example = "<XML>", required = true) @RequestBody String sampleResultXML) {
		logger.info("lims-service.LimsController.postLimsSampleResult1() started");
		var sampleResult = endurConnector.postBody("/SampleResults", String.class, sampleResultXML);
		logger.info("Post method postLimsResults completed return status :" + sampleResult);
		return sampleResult;
	}
	
//	@Cacheable({ "SampleResults" })
//	@ApiOperation("Update Lims SampleResult")
//	@PostMapping("/SampleResults")
//	public String postLimsSampleResult(@RequestBody com.fasterxml.jackson.databind.JsonNode payload
//			) {
//		
//		logger.info("Post Method postLimsSampleResult() started");
//		logger.info(payload);
//		
//		return "Success";
//	}
	
	@ExceptionHandler(Exception.class)
    public void  handleException(Exception exception) {
    	logger.error("lims-service.LimsController failed..." + exception.getMessage());
		for( StackTraceElement ste : exception.getStackTrace()) {
			logger.error(ste.toString());
		}
    }
}

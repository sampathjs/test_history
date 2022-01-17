package com.matthey.pmm.lims.service;

import com.olf.openrisk.application.EnumMessageSeverity;
import com.olf.openrisk.application.Session;
import com.matthey.pmm.lims.data.SampleUpdater;
import com.matthey.pmm.lims.LimsSampleResult;
import com.matthey.pmm.lims.data.ResultUpdater;
import com.matthey.pmm.lims.data.SampleResultUpdater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lims")
public class LimsController {

	private final Session session;

	private static final Logger logger = LogManager.getLogger(LimsController.class);

	public LimsController(Session session) {
		this.session = session;
		logger.info("LimsController constructor started", EnumMessageSeverity.Error);
	}

	@PostMapping("/Sample")
	String postLimsSample(@RequestParam String batchId, @RequestParam String sampleNumber,
			@RequestParam String product) {
		logger.info("lims-endur-connector.LimsController.postLimsSample with parameters batchId = {}, sampleNumber = {}, product = {}",
				batchId, sampleNumber, product);
		return new SampleUpdater(session).updateTable(batchId, sampleNumber, product);
	}

	@PostMapping("/Results")
	String postLimsResult(@RequestParam String result) {
		logger.info("lims-endur-connector.LimsController.postLimsResult with parameters result = {}", result);
		return new ResultUpdater(session).updateTable(result);
	}

	@PostMapping("/SampleResults")
	String postLimsSampleResults(@RequestBody String sampleResultXML) {
		logger.info("lims-endur-connector.LimsController.postLimsSampleResults");
		return new SampleResultUpdater(session).process(sampleResultXML);
	}

	@ExceptionHandler(Exception.class)
	public void handleException(Exception exception) {
		logger.error("lims-endur-connector.LimsController failed..." + exception.getMessage());
		for (StackTraceElement ste : exception.getStackTrace()) {
			logger.error(ste.toString());
		}
	}

}
